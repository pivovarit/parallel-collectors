package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author Grzegorz Piwowarek
 */
abstract class AbstractAsyncParallelCollector<T, R, C> implements Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>> {

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
}
