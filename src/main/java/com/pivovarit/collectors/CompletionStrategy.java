package com.pivovarit.collectors;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

sealed interface CompletionStrategy<T> extends Function<List<CompletableFuture<T>>, Stream<T>> permits CompletionStrategy.Unordered, CompletionStrategy.Ordered {

    Unordered<?> UNORDERED = new Unordered<>();
    Ordered<?> ORDERED = new Ordered<>();

    static <R> CompletionStrategy<R> unordered() {
        return (CompletionStrategy<R>) UNORDERED;
    }

    static <R> CompletionStrategy<R> ordered() {
        return (CompletionStrategy<R>) ORDERED;
    }

    final class Unordered<T> implements CompletionStrategy<T> {
        @Override
        public Stream<T> apply(List<CompletableFuture<T>> futures) {
            return StreamSupport.stream(new CompletionOrderSpliterator<>(futures), false);
        }
    }

    final class Ordered<T> implements CompletionStrategy<T> {
        @Override
        public Stream<T> apply(List<CompletableFuture<T>> futures) {
            return futures.stream().map(CompletableFuture::join);
        }
    }
}
