package com.pivovarit.utils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

public final class ParallelCollectors {
    private ParallelCollectors() {
    }

    public static <T> Supplier<T> supplier(Supplier<T> supplier) {
        return supplier;
    }

    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<List<T>>> toListInParallel(Executor executor) {
        return new ParallelCollectionCollector<>(executor);
    }

    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<List<T>>> toListInParallel(Executor executor, int parallelism) {
        return null; // TODO
    }

    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<List<T>>> toCollectionInParallel(Supplier<Collection> collection, Executor executor) {
        return null; // TODO
    }

    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<List<T>>> toCollectionInParallel(Supplier<Collection> collection, Executor executor, int parallelism) {
        return null; // TODO
    }

    public static <T> Collector<Supplier<T>, Stream<CompletableFuture<T>>, CompletableFuture<List<T>>> toStreamInParallel(Executor executor) {
        return null; // TODO
    }

    public static <T> Collector<Supplier<T>, Stream<CompletableFuture<T>>, CompletableFuture<List<T>>> toStreamInParallel(Executor executor, int parallelism) {
        return null; // TODO
    }
}