package com.pivovarit.collectors;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.parallelToList;
import static com.pivovarit.collectors.ParallelCollectors.parallelToSet;
import static com.pivovarit.collectors.infrastructure.TestUtils.incrementAndThrow;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.infrastructure.TestUtils.runWithExecutor;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static java.util.function.Function.identity;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author Grzegorz Piwowarek
 */
class MappingCollectorFunctionalTests {

    private static final Executor executor = Executors.newFixedThreadPool(100);

    @TestFactory
    Stream<DynamicTest> testCollectors() {
        return of(
          forCollector((mapper, e) -> parallelToSet(mapper, e), "parallelToSet(p=inf)"),
          forCollector((mapper, e) -> parallelToSet(mapper, e, 10), "parallelToSet(p=10)"),
          forCollector((mapper, e) -> parallelToList(mapper, e), "parallelToList(p=inf)"),
          forCollector((mapper, e) -> parallelToList(mapper, e, 10), "parallelToList(p=10)"),
          forCollector((mapper, e) -> parallelToCollection(mapper, ArrayList::new, e), "parallelToCollection(p=inf)"),
          forCollector((mapper, e) -> parallelToCollection(mapper, ArrayList::new, e, 10), "parallelToCollection(p=10)")
        ).flatMap(identity());
    }

    private static <T, R extends Collection<T>> Stream<DynamicTest> forCollector(BiFunction<Function<T, T>, Executor, Collector<T, List<CompletableFuture<T>>, CompletableFuture<R>>> collector, String name) {
        return of(
          shouldCollect(collector, name),
          shouldCollectToEmpty(collector, name),
          shouldNotBlockWhenReturningFuture(collector, name),
          shouldShortCircuitOnException(collector, name));
    }

    //@Test
    private static <T, R extends Collection<T>> DynamicTest shouldNotBlockWhenReturningFuture(BiFunction<Function<T, T>, Executor, Collector<T, List<CompletableFuture<T>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should not block when returning future", name), () -> {
            List<T> elements = (List<T>) IntStream.of().boxed().collect(Collectors.toList());
            assertTimeoutPreemptively(ofMillis(100), () ->
              elements.stream()
                .limit(5)
                .collect(collector.apply(i -> (T) returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), executor)));
        });
    }

    //@Test
    private static <T, R extends Collection<T>> DynamicTest shouldCollectToEmpty(BiFunction<Function<T, T>, Executor, Collector<T, List<CompletableFuture<T>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should collect to empty", name), () -> {
            List<T> elements = (List<T>) IntStream.of().boxed().collect(Collectors.toList());
            Collection<T> result11 = elements.stream().collect(collector.apply(i -> i, executor)).join();

            assertThat(result11)
              .isEmpty();
        });
    }

    //@Test
    private static <T, R extends Collection<T>> DynamicTest shouldCollect(BiFunction<Function<T, T>, Executor, Collector<T, List<CompletableFuture<T>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should collect", name), () -> {
            List<T> elements = (List<T>) IntStream.range(0, 10).boxed().collect(Collectors.toList());
            Collection<T> result = elements.stream().collect(collector.apply(i -> i, executor)).join();

            assertThat(result)
              .hasSameSizeAs(elements)
              .containsOnlyElementsOf(elements);
        });
    }

    //@Test
    private static <T, R extends Collection<T>> DynamicTest shouldShortCircuitOnException(BiFunction<Function<T, T>, Executor, Collector<T, List<CompletableFuture<T>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should short circuit on exception", name), () -> {
            List<T> elements = (List<T>) IntStream.range(0, 100).boxed().collect(Collectors.toList());
            int size = 4;

            runWithExecutor(e -> {
                // given
                LongAdder counter = new LongAdder();

                assertThatThrownBy(elements.stream()
                  .collect(collector.apply(i -> (T) incrementAndThrow(counter), e))::join)
                  .isInstanceOf(CompletionException.class)
                  .hasCauseExactlyInstanceOf(IllegalArgumentException.class);

                assertThat(counter.longValue()).isLessThanOrEqualTo(size);
            }, size);
        });
    }
}
