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

    abstract protected Runner dispatchStrategy();

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
                    && (task = getWorkingQueue().poll()) != null) {
                    dispatchStrategy().run(task);
                }
            } catch (Exception e) { // covers InterruptedException
                pending.forEach(future -> future.completeExceptionally(e));
            } catch (Throwable e) {
                pending.forEach(future -> future.completeExceptionally(e));
                throw e;
            }
        });
    }

    CompletableFuture<T> enqueue(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        pending.add(future);
        workingQueue.add(() -> {
            try {
                future.complete(supplier.get());
            } catch (Exception e) {
                handle(future, e);
            } catch (Throwable e) {
                handle(future, e);
                throw e;
            }
        });
        return future;
    }

    private void handle(CompletableFuture<T> future, Throwable e) {
        failed = true;
        future.completeExceptionally(e);
        pending.forEach(f -> f.obtrudeException(e));
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

    Queue<Runnable> getWorkingQueue() {
        return workingQueue;
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
    protected interface Runner {
        void run(Runnable task) throws Exception;
    }
}
