package com.pivovarit.collectors;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

class FutureCollectors {
    public static <T, R> Collector<CompletableFuture<T>, ?, CompletableFuture<R>> toFuture(Collector<T, ?, R> collector) {
        return Collectors.collectingAndThen(toList(), list -> {
            CompletableFuture<R> future = CompletableFuture
              .allOf(list.toArray(new CompletableFuture[0]))
              .thenApply(__ -> list.stream()
                .map(CompletableFuture::join)
                .collect(collector));

            for (CompletableFuture<T> f : list) {
                f.whenComplete((integer, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    }
                });
            }

            return future;
        });
    }

    public static <T> Collector<CompletableFuture<T>, ?, CompletableFuture<List<T>>> toFuture() {
        return toFuture(toList());
    }
}
