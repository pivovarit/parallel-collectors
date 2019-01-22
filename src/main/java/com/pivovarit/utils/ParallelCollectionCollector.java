package com.pivovarit.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class ParallelCollectionCollector<T>
  implements Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<List<T>>> {

    private final Executor executor;

    ParallelCollectionCollector(Executor executor) {
        this.executor = executor;
    }

    @Override
    public Supplier<List<CompletableFuture<T>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<CompletableFuture<T>>, Supplier<T>> accumulator() {
        return (processing, supplier) -> processing.add(CompletableFuture.supplyAsync(supplier, executor));
    }

    @Override
    public BinaryOperator<List<CompletableFuture<T>>> combiner() {
        return (left, right) -> {
            left.addAll(right);
            return left;
        };
    }

    @Override
    public Function<List<CompletableFuture<T>>, CompletableFuture<List<T>>> finisher() {
        return futures -> futures.stream().reduce(completedFuture(new ArrayList<>()),
          (list, object) -> list.thenCombine(object, (left, right) -> { left.add(right); return left; }),
          (f1, f2) -> f1.thenCombine(f2, (left, right) -> { left.addAll(right); return left; }));
    }

    @Override
    public Set<Characteristics> characteristics() {
        return new HashSet<>();
    }
}
