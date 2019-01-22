package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collector;

public final class ParallelCollectors {
    private ParallelCollectors() {
    }

    public static <T> Supplier<T> supplier(Supplier<T> supplier) {
        return supplier;
    }

    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<List<T>>> toListInParallel(Executor executor) {
        return new ParallelCollectionCollector<>(executor, ArrayList::new);
    }

    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<Set<T>>> toSetInParallel(Executor executor) {
        return new ParallelCollectionCollector<>(executor, HashSet::new);
    }
}
