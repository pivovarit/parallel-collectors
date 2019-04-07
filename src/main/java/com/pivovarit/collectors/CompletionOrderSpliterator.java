package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.util.concurrent.CompletableFuture.anyOf;

class CompletionOrderSpliterator<T> implements Spliterator<T> {

    private final List<CompletableFuture<T>> futureQueue;
    private final Runnable finisher;

    CompletionOrderSpliterator(List<CompletableFuture<T>> futures, Runnable finisher) {
        this.futureQueue = new ArrayList<>(futures);
        this.finisher = finisher;
    }

    CompletionOrderSpliterator(List<CompletableFuture<T>> futures) {
        this.futureQueue  = new ArrayList<>(futures);
        this.finisher = null;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (!futureQueue.isEmpty()) {
            T next = takeNextCompleted();
            action.accept(next);
            return true;
        } else {
            if (finisher != null) {
                finisher.run();
            }
            return false;
        }
    }

    private T takeNextCompleted() {
        anyOf(futureQueue.toArray(new CompletableFuture[0])).join();

        CompletableFuture<T> next = null;
        for (CompletableFuture<T> future : futureQueue) {
            if (future.isDone()) {
                next = future;
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


