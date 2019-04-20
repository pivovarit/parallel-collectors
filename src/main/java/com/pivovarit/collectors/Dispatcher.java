package com.pivovarit.collectors;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import static com.pivovarit.collectors.OnSpinWaitAdapter.onSpinWait;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * @author Grzegorz Piwowarek
 */
final class Dispatcher<T> {

    private final CompletableFuture<Void> completionSignaller = new CompletableFuture<>();

    private final ExecutorService dispatcher = newSingleThreadExecutor(new CustomThreadFactory());
    private final Queue<CompletableFuture<T>> pending = new ConcurrentLinkedQueue<>();
    private final Queue<Runnable> workingQueue = new ConcurrentLinkedQueue<>();
    private final Executor executor;

    private final Semaphore limiter;

    private volatile boolean completed = false;
    private volatile boolean started = false;
    private volatile boolean shortCircuited = false;

    Dispatcher(Executor executor) {
        this.executor = executor;
        this.limiter = new Semaphore(getDefaultParallelism());
    }

    Dispatcher(Executor executor, int permits) {
        this.executor = executor;
        this.limiter = new Semaphore(permits);
    }

    CompletableFuture<Void> start() {
        if (!started) {
            started = true;
        } else {
            return completionSignaller;
        }

        dispatcher.execute(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Runnable task = workingQueue.poll();
                    if (task == null && !completed) {
                        onSpinWait();
                        continue;
                    } else if (task == null && completed) {
                        break;
                    }
                    limiter.acquire();
                    runAsync(() -> {
                        try {
                            task.run();
                        } finally {
                            limiter.release();
                        }
                    }, executor);
                }

                completionSignaller.complete(null);
            } catch (InterruptedException e) {
                handle(e);
            } catch (Throwable e) {
                handle(e);
                throw e;
            }
        });
        try {
            return completionSignaller;
        } finally {
            dispatcher.shutdown();
        }
    }

    void stop() {
        completed = true;
    }

    boolean isRunning() {
        return started;
    }

    CompletableFuture<T> enqueue(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        pending.add(future);
        workingQueue.add(() -> {
            try {
                if (!shortCircuited) {
                    future.complete(supplier.get());
                    pending.remove(future);
                }
            } catch (Throwable e) {
                handle(e);
                throw e;
            }
        });
        return future;
    }

    private void handle(Throwable e) {
        pending.forEach(future -> future.completeExceptionally(e));
        completionSignaller.completeExceptionally(e);
        shortCircuited = true;
        dispatcher.shutdownNow();
    }

    boolean isEmpty() {
        return workingQueue.size() == 0;
    }

    /**
     * @author Grzegorz Piwowarek
     */
    private static class CustomThreadFactory implements ThreadFactory {

        private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = defaultThreadFactory.newThread(task);
            thread.setName("parallel-collector-" + thread.getName());
            thread.setDaemon(true);
            return thread;
        }
    }

    private static int getDefaultParallelism() {
        return Math.max(getRuntime().availableProcessors() - 1, 1);
    }
}
