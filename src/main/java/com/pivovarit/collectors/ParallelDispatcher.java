package com.pivovarit.collectors;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.pivovarit.collectors.AbstractParallelCollector.supplyWithResources;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

final class ParallelDispatcher<T> implements AutoCloseable {

    private final ExecutorService dispatcher = newSingleThreadExecutor(new CustomThreadFactory());
    private final Executor executor;
    private final Queue<Supplier<T>> workingQueue;
    private final Queue<CompletableFuture<T>> pendingQueue;
    private final Function<Queue<Supplier<T>>, Runnable> dispatchStrategy;

    private volatile boolean isFailed = false;

    ParallelDispatcher(Executor executor, Queue<Supplier<T>> workingQueue, Queue<CompletableFuture<T>> pendingQueue, Function<Queue<Supplier<T>>, Runnable> dispatchStrategy) {
        this.executor = executor;
        this.workingQueue = workingQueue;
        this.pendingQueue = pendingQueue;
        this.dispatchStrategy = dispatchStrategy;
    }

    @Override
    public void close() {
        dispatcher.shutdown();
    }

    void start() {
        dispatcher.execute(dispatchStrategy.apply(workingQueue));
    }

    CompletableFuture<T> enqueue(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        pendingQueue.add(future);
        workingQueue.add(() -> isMarkedFailed() ? null : supplier.get());
        return future;
    }

    CompletableFuture<T> run(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor)
          .whenComplete((r, throwable) -> {
              CompletableFuture<T> next = Objects.requireNonNull(pendingQueue.poll());
              if (throwable == null) {
                  next.complete(r);
              } else {
                  next.completeExceptionally(throwable);
                  isFailed = true;
              }
          });
    }

    CompletableFuture<T> run(Supplier<T> task, Runnable finisher) {
        return CompletableFuture.supplyAsync(task, executor).whenComplete((r, throwable) -> {
            CompletableFuture<T> next = Objects.requireNonNull(pendingQueue.poll());
            supplyWithResources(() -> throwable == null
                ? next.complete(r)
                : supplyWithResources(() -> next.completeExceptionally(throwable), () -> isFailed = true),
              finisher);
        });
    }

    void completePending(Exception e) {
        pendingQueue.forEach(future -> future.completeExceptionally(e));
    }

    void cancelPending() {
        pendingQueue.forEach(f -> f.cancel(true));
    }

    boolean isNotEmpty() {
        return workingQueue.size() != 0;
    }

    boolean isMarkedFailed() {
        return isFailed;
    }
}
