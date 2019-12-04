package com.pivovarit.collectors;

import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * @author Grzegorz Piwowarek
 */
final class CompletionOrderBatchingSpliterator<T> implements Spliterator<T> {

    private final int initialSize;
    private final BlockingQueue<CompletableFuture<T>> completed = new LinkedBlockingQueue<>();
    private int remaining;

    CompletionOrderBatchingSpliterator(List<CompletableFuture<List<T>>> futures) {
        this.initialSize = futures.size();
        this.remaining = initialSize;
        futures.forEach(batch -> {
            batch.whenComplete((ts, throwable) -> {
                if (throwable != null) {
                    CompletableFuture<T> failedFuture = new CompletableFuture<>();
                    failedFuture.completeExceptionally(throwable);
                    completed.add(failedFuture);
                } else {
                    ts.forEach(v -> completed.add(CompletableFuture.completedFuture(v)));
                }
            });
        });
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        return remaining > 0
          ? nextCompleted().thenAccept(action).thenApply(__ -> true).join()
          : false;
    }

    private CompletableFuture<T> nextCompleted() {
        remaining--;
        try {
            return completed.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return initialSize;
    }

    @Override
    public int characteristics() {
        return SIZED | IMMUTABLE | NONNULL;
    }
}


