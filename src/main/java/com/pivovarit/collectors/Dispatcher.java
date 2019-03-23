package com.pivovarit.collectors;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * @author Grzegorz Piwowarek
 */
abstract class Dispatcher<T> implements AutoCloseable {

    private final ExecutorService dispatcher = newSingleThreadExecutor(new CustomThreadFactory());
    private final Queue<CompletableFuture<T>> pendingQueue;
    private final Queue<Runnable> workingQueue;
    private final Executor executor;

    private volatile boolean failed = false;

    Dispatcher(Executor executor) {
        this.executor = executor;
        this.workingQueue = new ConcurrentLinkedQueue<>();
        this.pendingQueue = new ConcurrentLinkedQueue<>();
    }

    abstract protected Runnable dispatchStrategy();

    @Override
    public void close() {
        dispatcher.shutdown();
    }

    void start() {
        dispatcher.execute(dispatchStrategy());
    }

    CompletableFuture<T> enqueue(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        pendingQueue.add(future);
        workingQueue.add(() -> {
            try {
                T result = supplier.get();
                future.complete(result);
            } catch (Throwable e) {
                failed = true;
                future.completeExceptionally(e);
                pendingQueue.forEach(f -> f.completeExceptionally(e));
                throw e;
            }
        });
        return future;
    }

    CompletableFuture<Void> run(Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }

    CompletableFuture<Void> run(Runnable task, Runnable finisher) {
        return CompletableFuture.runAsync(task, executor)
          .whenComplete((r, throwable) -> finisher.run());
    }

    Queue<Runnable> getWorkingQueue() {
        return workingQueue;
    }

    void completePending(Exception e) {
        pendingQueue.forEach(future -> future.completeExceptionally(e));
    }

    boolean isFailed() {
        return failed;
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
}
