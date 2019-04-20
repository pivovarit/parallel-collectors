package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.util.concurrent.CompletableFuture.anyOf;

/**
 * @author Grzegorz Piwowarek
 */
final class CompletionOrderSpliterator<T> implements Spliterator<T> {

    private final List<CompletableFuture<T>> futureQueue;

    CompletionOrderSpliterator(List<CompletableFuture<T>> futures) {
        this.futureQueue  = new ArrayList<>(futures);
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (!futureQueue.isEmpty()) {
            T next = takeNextCompleted();
            action.accept(next);
            return true;
        } else {
            return false;
        }
    }

    private T takeNextCompleted() {
        anyOf(futureQueue.toArray(new CompletableFuture[0])).join();

        CompletableFuture<T> next = null;
        int index = -1;
        for (int i = 0; i < futureQueue.size(); i++) {
            CompletableFuture<T> future = futureQueue.get(i);
            if (future.isDone()) {
                next = future;
                index = i;
                break;
            }
        }
        futureQueue.remove(index);
        return next.join();
    }

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return futureQueue.size();
    }

    @Override
    public int characteristics() {
        return SIZED & IMMUTABLE;
    }
}


