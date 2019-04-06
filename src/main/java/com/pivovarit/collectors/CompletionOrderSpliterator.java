package com.pivovarit.collectors;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.util.concurrent.CompletableFuture.anyOf;

class CompletionOrderSpliterator<T> implements Spliterator<T> {

    private final Set<CompletableFuture<T>> futureQueue;
    private final Runnable finisher;

    CompletionOrderSpliterator(Collection<CompletableFuture<T>> futures, Runnable finisher) {
        this.futureQueue = new HashSet<>(futures);
        this.finisher = finisher;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (!futureQueue.isEmpty()) {
            T next = takeNextCompleted();
            action.accept(next);
            return true;
        } else {
            finisher.run();
            return false;
        }
    }

    private T takeNextCompleted() {
        anyOf(futureQueue.toArray(new CompletableFuture[0])).join();

        CompletableFuture<T> next = null;
        for (CompletableFuture<T> tCompletableFuture : futureQueue) {
            if (tCompletableFuture.isDone()) {
                next = tCompletableFuture;
                break;
            }
        }
        futureQueue.remove(next);

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
