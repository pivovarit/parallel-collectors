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
final class CompletionOrderSpliterator<T> implements Spliterator<T> {

    private final int initialSize;
    private final BlockingQueue<CompletableFuture<T>> completed = new LinkedBlockingQueue<>();
    private int remaining;

    private CompletionOrderSpliterator(int size) {
        this.initialSize = size;
        this.remaining = initialSize;
    }

    static <T> Spliterator<T> instance(List<CompletableFuture<T>> futures) {
        return new CompletionOrderSpliterator<T>(futures.size()).from(futures);
    }

    static <T> Spliterator<T> batching(List<CompletableFuture<List<T>>> futures) {
        return new CompletionOrderSpliterator<T>(futures.size()).fromPartitioned(futures);
    }

    private CompletionOrderSpliterator<T> from(List<CompletableFuture<T>> futures) {
        futures.forEach(f -> f.whenComplete((t, __) -> this.nextCompleted(f)));
        return this;
    }

    private CompletionOrderSpliterator<T> fromPartitioned(List<CompletableFuture<List<T>>> futures) {
        futures.forEach(f -> f.whenComplete((t, ex) -> {
            if (ex == null) {
                t.forEach(inner -> this.nextCompleted(f.thenApply(list -> inner)));
            } else {
                CompletableFuture<T> exFuture = new CompletableFuture<>();
                exFuture.completeExceptionally(ex);
                this.nextCompleted(exFuture);
            }
        }));
        return this;
    }

    private void nextCompleted(CompletableFuture<T> future) {
        completed.add(future);
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


