package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;


/**
 * @author Grzegorz Piwowarek
 */
class OrderedFutureSpliterator<T> implements Spliterator<T> {

    private final List<CompletableFuture<T>> futureQueue;
    private final Runnable finisher;

    OrderedFutureSpliterator(List<CompletableFuture<T>> futures, Runnable finisher) {
        this.futureQueue = new ArrayList<>(futures);
        this.finisher = finisher;
    }

    OrderedFutureSpliterator(List<CompletableFuture<T>> futures) {
        this.futureQueue = new ArrayList<>(futures);
        this.finisher = null;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (!futureQueue.isEmpty()) {
            T next = futureQueue.remove(0).join();
            action.accept(next);
            return true;
        } else {
            if (finisher != null) {
                finisher.run();
            }
            return false;
        }
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


