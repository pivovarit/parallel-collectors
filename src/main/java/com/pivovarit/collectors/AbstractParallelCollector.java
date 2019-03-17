package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * @author Grzegorz Piwowarek
 */
abstract class AbstractParallelCollector<T, R, C>
  implements Collector<T, List<CompletableFuture<R>>, C> {

    @Override
    public Supplier<List<CompletableFuture<R>>> supplier() {
        return () -> Collections.synchronizedList(new ArrayList<>());
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
        return futures -> futures.stream()
          .reduce(completedFuture(collectionFactory.get()),
            accumulatingResults(),
            mergingPartialResults());
    }

    private static <T1, R1 extends Collection<T1>> BinaryOperator<CompletableFuture<R1>> mergingPartialResults() {
        return (f1, f2) -> f1.thenCombine(f2, (left, right) -> {
            left.addAll(right);
            return left;
        });
    }

    private static <T1, R1 extends Collection<T1>> BiFunction<CompletableFuture<R1>, CompletableFuture<T1>, CompletableFuture<R1>> accumulatingResults() {
        return (list, object) -> list.thenCombine(object, (left, right) -> {
            left.add(right);
            return left;
        });
    }
}
