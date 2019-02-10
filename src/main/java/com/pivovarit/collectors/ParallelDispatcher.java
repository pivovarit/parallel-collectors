package com.pivovarit.collectors;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

final class ParallelDispatcher<T> implements AutoCloseable {

    private final ExecutorService dispatcher = newSingleThreadExecutor(new CustomThreadFactory());
    private final Executor executor;
    private final Queue<Supplier<T>> workingQueue;
    private final Queue<CompletableFuture<T>> pendingQueue;
    private final Function<Queue<Supplier<T>>, Runnable> dispatchStrategy;


    ParallelDispatcher(Executor executor, Queue<Supplier<T>> workingQueue, Queue<CompletableFuture<T>> pendingQueue, Function<Queue<Supplier<T>>, Runnable> dispatchStrategy) {
        this.executor = executor;
        this.workingQueue = workingQueue;
        this.pendingQueue = pendingQueue;
        this.dispatchStrategy = dispatchStrategy;
    }

    void addPending(CompletableFuture<T> future) {
        pendingQueue.add(future);
    }

    void addTask(Supplier<T> supplier) {
        workingQueue.add(supplier);
    }

    @Override
    public void close() {
        dispatcher.shutdown();
    }

    CompletableFuture<T> supply(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    CompletableFuture<T> nextPending() {
        return pendingQueue.poll();
    }

    void closeExceptionally(Exception e) {
        pendingQueue.forEach(future -> future.completeExceptionally(e));
    }

    void cancelAll() {
        pendingQueue.forEach(f -> f.cancel(true));
    }

    boolean isNotEmpty() {
        return workingQueue.size() != 0;
    }

    void start() {
        dispatcher.execute(dispatchStrategy.apply(workingQueue));
    }
}
