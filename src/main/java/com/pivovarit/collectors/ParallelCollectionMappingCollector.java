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

class ParallelCollectionMappingCollector<T1, T2, R extends Collection<T2>>
  implements Collector<T1, List<CompletableFuture<T2>>, CompletableFuture<R>> {

    private final Executor executor;
    private final Supplier<R> collectionSupplier;
    private final Function<T1, T2> operation;

    ParallelCollectionMappingCollector(Function<T1, T2> operation, Executor executor, Supplier<R> collection) {
        this.executor = executor;
        this.collectionSupplier = collection;
        this.operation = operation;
    }

    @Override
    public Supplier<List<CompletableFuture<T2>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<CompletableFuture<T2>>, T1> accumulator() {
        return (processing, e) -> processing.add(supplyAsync(() -> operation.apply(e), executor));
    }

    @Override
    public BinaryOperator<List<CompletableFuture<T2>>> combiner() {
        return (left, right) -> {
            left.addAll(right);
            return left;
        };
    }

    @Override
    public Function<List<CompletableFuture<T2>>, CompletableFuture<R>> finisher() {
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
