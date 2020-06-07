package com.pivovarit.collectors;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

interface CompletionStrategy<T> extends Function<Stream<CompletableFuture<T>>, Stream<T>> {

    static <R> CompletionStrategy<R> unordered() {
        return futures -> StreamSupport.stream(new CompletionOrderSpliterator<>(futures), false);
    }

    static <R> CompletionStrategy<R> ordered() {
        return futures -> futures.map(CompletableFuture::join);
    }
}
