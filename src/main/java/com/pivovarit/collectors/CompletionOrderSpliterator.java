package com.pivovarit.collectors;

import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Grzegorz Piwowarek
 */
final class CompletionOrderSpliterator<T> implements Spliterator<T> {

    private final int initialSize;
    private final BlockingQueue<CompletableFuture<T>> completed = new LinkedBlockingQueue<>();
    private int remaining;

    CompletionOrderSpliterator(Stream<CompletableFuture<T>> futures) {
        AtomicInteger size = new AtomicInteger();
        futures.forEach(f -> {
            f.whenComplete((__, ___) -> completed.add(f));
            size.incrementAndGet();
        });

        this.initialSize = size.get();
        this.remaining = initialSize;
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


