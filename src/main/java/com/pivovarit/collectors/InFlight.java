package com.pivovarit.collectors;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

class InFlight<T> {
    private final Queue<CompletableFuture<T>> pending = new ConcurrentLinkedQueue<>();
    private final Queue<Future<?>> cancellables = new ConcurrentLinkedQueue<>();

    void registerPending(CompletableFuture<T> future) {
        pending.add(future);
    }

    void registerCancellable(Future<Void> future) {
        cancellables.add(future);
    }

    void cancelAll() {
        cancellables.forEach(future -> future.cancel(true));
    }

    void completeExceptionally(Throwable e) {
        pending.forEach(future -> future.completeExceptionally(e));
    }
}
