package com.pivovarit.collectors;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author Grzegorz Piwowarek
 */
final class FutureCollectors {
    static <T, R> Collector<CompletableFuture<T>, ?, CompletableFuture<R>> toFuture(Collector<T, ?, R> collector) {
        return Collectors.collectingAndThen(toList(), list -> {
            var future = CompletableFuture.allOf(list.toArray(CompletableFuture[]::new))
              .thenApply(__ -> list.stream()
                .map(CompletableFuture::join)
                .collect(collector));

            for (var f : list) {
                f.whenComplete((__, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    }
                });
            }

            return future;
        });
    }

    static <T> Collector<CompletableFuture<T>, ?, CompletableFuture<List<T>>> toFuture() {
        return toFuture(toList());
    }
}
