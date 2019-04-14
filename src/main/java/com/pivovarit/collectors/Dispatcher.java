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

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * @author Grzegorz Piwowarek
 */
class Dispatcher<T> {

    private final ExecutorService dispatcher = newSingleThreadExecutor(new CustomThreadFactory());
    private final Queue<CompletableFuture<T>> pending = new ConcurrentLinkedQueue<>();
    private final Queue<Runnable> workingQueue = new ConcurrentLinkedQueue<>();
    private final Executor executor;

    private final Semaphore limiter;
    private volatile boolean completed = false;

    Dispatcher(Executor executor) {
        this.executor = executor;
        this.limiter = new Semaphore(getDefaultParallelism());
    }

    Dispatcher(Executor executor, int permits) {
        this.executor = executor;
        this.limiter = new Semaphore(permits);
    }

    void start() {
        dispatcher.execute(() -> {
            Runnable task;
            try {
                while (
                  !Thread.currentThread().isInterrupted()
                    && !completed
                    && (task = workingQueue.poll()) != null) {

                    limiter.acquire();
                    run(task, limiter::release);
                }
            } catch (Exception e) { // covers InterruptedException
                handle(e);
            } catch (Throwable e) {
                handle(e);
                throw e;
            }
        });
        dispatcher.shutdown();
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
        completed = true;
        pending.forEach(future -> future.completeExceptionally(e));
        limiter.release();
    }

    private void run(Runnable task, Runnable finisher) {
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
    private static int getDefaultParallelism() {
        return getRuntime().availableProcessors() != 1 ? getRuntime().availableProcessors() - 1 : 1;
    }
}
