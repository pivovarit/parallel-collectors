package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Collections.synchronizedList;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toCollection;

/**
 * @author Grzegorz Piwowarek
 */
abstract class AbstractAsyncCollector<T, R, C>
  implements Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>> {

    @Override
    public Supplier<List<CompletableFuture<R>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BinaryOperator<List<CompletableFuture<R>>> combiner() {
        return (left, right) -> {
            left.addAll(right);
            return left;
        };
    }

    static <T1> T1 supplyWithResources(Supplier<T1> supplier, Runnable action) {
        try {
            return supplier.get();
        } finally {
            action.run();
        }
    }

    static <R, C extends Collection<R>> Function<List<CompletableFuture<R>>, CompletableFuture<C>> foldLeftFutures(Supplier<C> collectionFactory) {
        return futures -> allOf(futures.toArray(new CompletableFuture<?>[0]))
          .thenApply(__ -> futures.stream()
            .map(CompletableFuture::join)
            .collect(toCollection(collectionFactory)));
    }
}
