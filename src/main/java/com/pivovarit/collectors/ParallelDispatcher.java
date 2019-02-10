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


    public ParallelDispatcher(Executor executor, Queue<Supplier<T>> workingQueue, Queue<CompletableFuture<T>> pendingQueue, Function<Queue<Supplier<T>>, Runnable> dispatchStrategy) {
        this.executor = executor;
        this.workingQueue = workingQueue;
        this.pendingQueue = pendingQueue;
        this.dispatchStrategy = dispatchStrategy;
    }

    public void addPending(CompletableFuture<T> future) {
        pendingQueue.add(future);
    }

    public void addTask(Supplier<T> supplier) {
        workingQueue.add(supplier);
    }

    @Override
    public void close() {
        dispatcher.shutdown();
    }

    public CompletableFuture<T> supply(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    public CompletableFuture<T> nextPending() {
        return pendingQueue.poll();
    }

    public void closeExceptionally(Exception e) {
        pendingQueue.forEach(future -> future.completeExceptionally(e));
    }

    public void cancelAll() {
        pendingQueue.forEach(f -> f.cancel(true));
    }

    public boolean isNotEmpty() {
        return workingQueue.size() != 0;
    }

    public void start() {
        dispatcher.execute(dispatchStrategy.apply(workingQueue));
    }
}
