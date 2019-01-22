package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

class ParallelCollectionCollector<T, R extends Collection<T>>
  implements Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<R>> {

    private final Executor executor;
    private final Supplier<R> collectionSupplier;

    ParallelCollectionCollector(Executor executor, Supplier<R> collection) {
        this.executor = executor;
        this.collectionSupplier = collection;
    }

    @Override
    public Supplier<List<CompletableFuture<T>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<CompletableFuture<T>>, Supplier<T>> accumulator() {
        return (processing, supplier) -> processing.add(supplyAsync(supplier, executor));
    }

    @Override
    public BinaryOperator<List<CompletableFuture<T>>> combiner() {
        return (left, right) -> {
            left.addAll(right);
            return left;
        };
    }

    @Override
    public Function<List<CompletableFuture<T>>, CompletableFuture<R>> finisher() {
        return futures -> futures.stream()
          .reduce(completedFuture(collectionSupplier.get()),
            accumulatingResults(),
            mergingPartialResults());
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }

    private static <T1, R1 extends Collection<T1>> BinaryOperator<CompletableFuture<R1>> mergingPartialResults() {
        return (f1, f2) -> f1.thenCombine(f2, (left, right) -> {
            left.addAll(right);
            return left;
        });
    }

    private static <T1, R1 extends Collection<T1> > BiFunction<CompletableFuture<R1>, CompletableFuture<T1>, CompletableFuture<R1>> accumulatingResults() {
        return (list, object) -> list.thenCombine(object, (left, right) -> {
            left.add(right);
            return left;
        });
    }
}
