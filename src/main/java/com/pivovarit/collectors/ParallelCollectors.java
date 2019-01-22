package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public final class ParallelCollectors {
    private ParallelCollectors() {
    }

    public static <T> Supplier<T> supplier(Supplier<T> supplier) {
        return supplier;
    }

    public static <T, R extends Collection<T>> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<R>> toCollectionInParallel(Supplier<R> collection, Executor executor) {
        return new ParallelMappingCollector<>(Supplier::get, executor, collection);
    }

    public static <T, R, C extends Collection<R>> Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>> toCollectionInParallel(Function<T, R> operation, Supplier<C> collection, Executor executor) {
        return new ParallelMappingCollector<>(operation, executor, collection);
    }

    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<List<T>>> toListInParallel(Executor executor) {
        return new ParallelMappingCollector<>(Supplier::get, executor, ArrayList::new);
    }

    public static <T, R> Collector<T, List<CompletableFuture<R>>, CompletableFuture<List<R>>> toListInParallel(Function<T, R> operation, Executor executor) {
        return new ParallelMappingCollector<>(operation, executor, ArrayList::new);
    }

    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<Set<T>>> toSetInParallel(Executor executor) {
        return new ParallelMappingCollector<>(Supplier::get, executor, HashSet::new);
    }

    public static <T, R> Collector<T, List<CompletableFuture<R>>, CompletableFuture<Set<R>>> toSetInParallel(Function<T, R> operation, Executor executor) {
        return new ParallelMappingCollector<>(operation, executor, HashSet::new);
    }
}
