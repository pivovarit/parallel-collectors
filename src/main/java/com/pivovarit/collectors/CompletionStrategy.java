package com.pivovarit.collectors;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

sealed interface CompletionStrategy<T>
  extends Function<List<CompletableFuture<T>>, Stream<T>>
  permits CompletionStrategy.Unordered, CompletionStrategy.Ordered {

    static <R> CompletionStrategy<R> unordered() {
        return new Unordered<>();
    }

    static <R> CompletionStrategy<R> ordered() {
        return new Ordered<>();
    }

    final class Unordered<R> implements CompletionStrategy<R> {

        @Override
        public Stream<R> apply(List<CompletableFuture<R>> futures) {
            return StreamSupport.stream(new CompletionOrderSpliterator<>(futures), false);
        }
    }

    final class Ordered<R> implements CompletionStrategy<R> {

        @Override
        public Stream<R> apply(List<CompletableFuture<R>> futures) {
            return futures.stream().map(CompletableFuture::join);
        }
    }
}
