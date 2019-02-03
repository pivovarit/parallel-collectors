package com.pivovarit.collectors;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.parallelToList;
import static com.pivovarit.collectors.ParallelCollectors.parallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static java.util.function.Function.identity;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author Grzegorz Piwowarek
 */
class CollectorFunctionalTests {

    private static final Executor executor = Executors.newFixedThreadPool(100);

    @TestFactory
    Stream<DynamicTest> testMappingCollectors() {
        return of(
          forCollector(e -> parallelToSet(e), "parallelToSet(p=inf)"),
          forCollector(e -> parallelToSet(e, 10), "parallelToSet(p=10)"),
          forCollector(e -> parallelToList(e), "parallelToList(p=inf)"),
          forCollector(e -> parallelToList(e, 10), "parallelToList(p=10)"),
          forCollector(e -> parallelToCollection(ArrayList::new, e), "parallelToCollection(p=inf)"),
          forCollector(e -> parallelToCollection(ArrayList::new, e, 10), "parallelToCollection(p=10)")
        ).flatMap(identity());
    }

    private static <T, R extends Collection<T>> Stream<DynamicTest> forCollector(Function<Executor, Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<R>>> collector, String name) {
        return of(
          shouldCollect(collector, name),
          shouldCollectToEmpty(collector, name),
          shouldNotBlockWhenReturningFuture(collector, name));
    }

    //@Test
    private static <T, R extends Collection<T>> DynamicTest shouldNotBlockWhenReturningFuture(Function<Executor, Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should not block when returning future", name), () -> {
            List<T> elements = (List<T>) IntStream.of().boxed().collect(Collectors.toList());
            assertTimeoutPreemptively(ofMillis(100), () ->
              elements.stream()
                .limit(5)
                .map(i -> supplier(() -> (T) returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
                .collect(collector.apply(executor)));
        });
    }

    //@Test
    private static <T, R extends Collection<T>> DynamicTest shouldCollectToEmpty(Function<Executor, Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should collect to empty", name), () -> {
            List<T> elements = (List<T>) IntStream.of().boxed().collect(Collectors.toList());
            Collection<T> result11 = elements.stream()
              .map(i -> supplier(() -> i))
              .collect(collector.apply(executor)).join();

            assertThat(result11)
              .isEmpty();
        });
    }

    //@Test
    private static <T, R extends Collection<T>> DynamicTest shouldCollect(Function<Executor, Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should collect", name), () -> {
            List<T> elements = (List<T>) IntStream.range(0, 10).boxed().collect(Collectors.toList());
            Collection<T> result = elements.stream()
              .map(i -> supplier(() -> i))
              .collect(collector.apply(executor)).join();

            assertThat(result)
              .hasSameSizeAs(elements)
              .containsOnlyElementsOf(elements);
        });
    }
}
