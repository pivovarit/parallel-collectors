package com.pivovarit.collectors;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.util.concurrent.CompletableFuture.anyOf;

/**
 * @author Grzegorz Piwowarek
 */
final class CompletionOrderSpliterator<T> implements Spliterator<T> {

    private final Map<Integer, CompletableFuture<Map.Entry<Integer, T>>> indexAwareFutureMap = new HashMap<>();

    CompletionOrderSpliterator(List<CompletableFuture<T>> futures) {
        populateFutureMap(futures);
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (!indexAwareFutureMap.isEmpty()) {
            action.accept(nextCompleted());
            return true;
        } else {
            return false;
        }
    }

    private T nextCompleted() {
        return anyOf(indexAwareFutureMap.values().toArray(new CompletableFuture[0]))
          .thenApply(result -> ((Map.Entry<Integer, T>) result))
          .thenApply(result -> {
              indexAwareFutureMap.remove(result.getKey());
              return result.getValue();
          }).join();
    }

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return indexAwareFutureMap.size();
    }

    @Override
    public int characteristics() {
        return SIZED & IMMUTABLE;
    }

    private void populateFutureMap(List<CompletableFuture<T>> futures) {
        int counter = 0;
        for (CompletableFuture<T> f : futures) {
            int i = counter++;
            indexAwareFutureMap.put(i, f.thenApply(value -> new AbstractMap.SimpleEntry<>(i, value)));
        }
    }
}


