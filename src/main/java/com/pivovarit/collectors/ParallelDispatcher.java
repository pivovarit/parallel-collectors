package com.pivovarit.collectors;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

final class ParallelDispatcher<T> {

    final ExecutorService dispatcher = newSingleThreadExecutor(new CustomThreadFactory());
    final Executor executor;
    final Queue<Supplier<T>> workingQueue;
    final Queue<CompletableFuture<T>> pendingQueue;

    ParallelDispatcher(Executor executor, Queue<Supplier<T>> workingQueue, Queue<CompletableFuture<T>> pendingQueue) {
        this.executor = executor;
        this.workingQueue = workingQueue;
        this.pendingQueue = pendingQueue;
    }
}
