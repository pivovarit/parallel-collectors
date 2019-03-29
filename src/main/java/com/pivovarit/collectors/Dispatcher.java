package com.pivovarit.collectors;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * @author Grzegorz Piwowarek
 */
abstract class Dispatcher<T> implements AutoCloseable {

    private final ExecutorService dispatcher = newSingleThreadExecutor(new CustomThreadFactory());
    private final Queue<CompletableFuture<T>> pending;
    private final Queue<Runnable> workingQueue;
    private final Executor executor;

    private volatile boolean failed = false;

    Dispatcher(Executor executor) {
        this.executor = executor;
        this.workingQueue = new ConcurrentLinkedQueue<>();
        this.pending = new ConcurrentLinkedQueue<>();
    }

    abstract protected CheckedConsumer dispatchStrategy();

    @Override
    public void close() {
        dispatcher.shutdown();
    }

    void start() {
        dispatcher.execute(() -> {
            Runnable task;
            try {
                while (
                  !Thread.currentThread().isInterrupted()
                    && !failed
                    && (task = workingQueue.poll()) != null) {
                    dispatchStrategy().consume(task);
                }
            } catch (Exception e) { // covers InterruptedException
                completeRemaining(e);
            } catch (Throwable e) {
                completeRemaining(e);
                throw e;
            }
        });
    }

    private void completeRemaining(Throwable e) {
        pending.forEach(future -> future.completeExceptionally(e));
    }

    CompletableFuture<T> enqueue(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        pending.add(future);
        workingQueue.add(() -> {
            try {
                future.complete(supplier.get());
                pending.remove(future);
            } catch (Exception e) {
                handle(e);
            } catch (Throwable e) {
                handle(e);
                throw e;
            }
        });
        return future;
    }

    private void handle(Throwable e) {
        failed = true;
        completeRemaining(e);

    }

    void run(Runnable task) {
        runAsync(task, executor);
    }

    void run(Runnable task, Runnable finisher) {
        runAsync(() -> {
            try {
                task.run();
            } finally {
                finisher.run();
            }
        }, executor);
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

    @FunctionalInterface
    protected interface CheckedConsumer {
        void consume(Runnable task) throws Exception;
    }
}
