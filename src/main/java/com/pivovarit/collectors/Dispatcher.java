package com.pivovarit.collectors;

import java.util.Objects;
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
    private final Queue<Supplier<T>> workingQueue;
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
        workingQueue.add(() -> isFailed() ? null : supplier.get());
        return future;
    }

    void run(Supplier<T> task) {
        CompletableFuture.supplyAsync(task, executor).whenComplete((r, throwable) -> {
            CompletableFuture<T> next = Objects.requireNonNull(pendingQueue.poll(), "internal error, future can't be null");
            if (throwable == null) {
                next.complete(r);
            } else {
                next.completeExceptionally(throwable);
                cancelPending();
                failed = true;
            }
        });
    }

    void run(Supplier<T> task, Runnable finisher) {
        CompletableFuture.supplyAsync(task, executor).whenComplete((r, throwable) -> {
            CompletableFuture<T> next = Objects.requireNonNull(pendingQueue.poll());
            try {
                if (throwable == null) {
                    next.complete(r);
                } else {
                    next.completeExceptionally(throwable);
                    cancelPending();
                    failed = true;
                }
            } finally {
                finisher.run();
            }
        });
    }

    Queue<Supplier<T>> getWorkingQueue() {
        return workingQueue;
    }

    void completePending(Exception e) {
        pendingQueue.forEach(future -> future.completeExceptionally(e));
    }

    private void cancelPending() {
        pendingQueue.forEach(f -> f.cancel(true));
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
