package com.pivovarit.collectors;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.parallelToList;
import static com.pivovarit.collectors.ParallelCollectors.parallelToSet;
import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author Grzegorz Piwowarek
 */
class SmokeTests {

    private static final Executor executor = Executors.newFixedThreadPool(100);

    @TestFactory
    Stream<DynamicTest> testCollectors() {
        return of(
          forCollector(parallelToSet(i -> i, executor), "parallelToSet(p=inf)"),
          forCollector(parallelToSet(i -> i, executor, 10), "parallelToSet(p=10)"),
          forCollector(parallelToList(i -> i, executor), "parallelToList(p=inf)"),
          forCollector(parallelToList(i -> i, executor, 10), "parallelToList(p=10)"),
          forCollector(parallelToCollection(i -> i, ArrayList::new, executor), "parallelToCollection(p=inf)"),
          forCollector(parallelToCollection(i -> i, ArrayList::new, executor, 10), "parallelToCollection(p=10)")
        ).flatMap(identity());
    }

    private static <T, R extends Collection<T>> Stream<DynamicTest> forCollector(Collector<T, List<CompletableFuture<T>>, CompletableFuture<R>> collector, String name) {
        return of(
          shouldCollect(collector, name),
          shouldCollectToEmpty(collector, name));
    }

    private static <T, R extends Collection<T>> DynamicTest shouldCollect(Collector<T, List<CompletableFuture<T>>, CompletableFuture<R>> collector, String name) {
        return dynamicTest(format("%s: should collect", name), () -> {
            List<T> elements = (List<T>) IntStream.range(0, 10).boxed().collect(Collectors.toList());
            Collection<T> result = elements.stream().collect(collector).join();

            assertThat(result)
              .hasSameSizeAs(elements)
              .containsOnlyElementsOf(elements);
        });
    }

    private static <T, R extends Collection<T>> DynamicTest shouldCollectToEmpty(Collector<T, List<CompletableFuture<T>>, CompletableFuture<R>> collector, String name) {
        return dynamicTest(format("%s: should collect to empty", name), () -> {
            List<T> elements = (List<T>) IntStream.of().boxed().collect(Collectors.toList());
            Collection<T> result11 = elements.stream().collect(collector).join();

            assertThat(result11)
              .isEmpty();
        });
    }
}
