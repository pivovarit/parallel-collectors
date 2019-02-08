package com.pivovarit.collectors;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.LongAdder;
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
class CollectorFunctionalTest {

    private static final Executor executor = Executors.newFixedThreadPool(100);

    @TestFactory
    Stream<DynamicTest> testMappingCollectors() {
        return of(
          forCollector(e -> parallelToSet(e), "parallelToSet(p=inf)"),
          forCollector(e -> parallelToSet(e, 1000), "parallelToSet(p=1000)"),
          forCollector(e -> parallelToList(e), "parallelToList(p=inf)"),
          forCollector(e -> parallelToList(e, 1000), "parallelToList(p=1000)"),
          forCollector(e -> parallelToCollection(ArrayList::new, e), "parallelToCollection(p=inf)"),
          forCollector(e -> parallelToCollection(ArrayList::new, e, 1000), "parallelToCollection(p=1000)")
        ).flatMap(identity());
    }

    private static <T, R extends Collection<T>> Stream<DynamicTest> forCollector(Function<Executor, Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<R>>> collector, String name) {
        return of(
          shouldCollect(collector, name),
          shouldCollectToEmpty(collector, name),
          shouldNotBlockWhenReturningFuture(collector, name),
          shouldShortCircuitOnException(collector, name),
          shouldNotSwallowException(collector, name),
          shouldSurviveRejectedExecutionException(collector, name),
          shouldBeConsistent(collector, name));
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

    //@Test
    private static <T, R extends Collection<T>> DynamicTest shouldShortCircuitOnException(Function<Executor, Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should short circuit on exception", name), () -> {
            List<T> elements = (List<T>) IntStream.range(0, 100).boxed().collect(Collectors.toList());
            int size = 4;

            runWithExecutor(e -> {
                // given
                LongAdder counter = new LongAdder();

                assertThatThrownBy(elements.stream()
                  .map(i -> supplier(() -> (T) incrementAndThrow(counter)))
                  .collect(collector.apply(e))::join)
                  .isInstanceOf(CompletionException.class)
                  .hasCauseExactlyInstanceOf(IllegalArgumentException.class);

                assertThat(counter.longValue()).isLessThanOrEqualTo(size);
            }, size);
        });
    }

    //@Test
    private static <T, R extends Collection<T>> DynamicTest shouldNotSwallowException(Function<Executor, Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should not swallow exception", name), () -> {
            List<T> elements = (List<T>) IntStream.range(0, 10).boxed().collect(Collectors.toList());

            runWithExecutor(e -> {
                assertThatThrownBy(elements.stream()
                  .map(i -> supplier(() -> {
                      if ((Integer) i == 7) {
                          throw new IllegalArgumentException();
                      } else {
                          return i;
                      }
                  }))
                  .collect(collector.apply(e))::join)
                  .isInstanceOf(CompletionException.class)
                  .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
            }, 10);
        });
    }

    //@Test
    private static <T, R extends Collection<T>> DynamicTest shouldSurviveRejectedExecutionException(Function<Executor, Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s:{ should not swallow exception", name), () -> {
            Executor executor = command -> { throw new RejectedExecutionException(); };
            List<T> elements = (List<T>) IntStream.range(0, 1000).boxed().collect(Collectors.toList());

            assertThatThrownBy(() -> elements.stream()
              .map(i -> supplier(() -> returnWithDelay(i, ofMillis(10000))))
              .collect(collector.apply(executor))
              .join())
              .isInstanceOf(CompletionException.class)
              .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
        });
    }

    //@Test
    private static <T, R extends Collection<T>> DynamicTest shouldBeConsistent(Function<Executor, Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s:{ should remain consistent", name), () -> {
            ExecutorService executor = Executors.newFixedThreadPool(1000);
            try {
                List<T> elements = (List<T>) IntStream.range(0, 1000).boxed().collect(Collectors.toList());

                CountDownLatch countDownLatch = new CountDownLatch(1000);

                R result = elements.stream()
                  .map(i -> supplier(() -> {
                      countDownLatch.countDown();
                      try {
                          countDownLatch.await();
                      } catch (InterruptedException e) {
                          throw new RuntimeException(e);
                      }
                      return i;
                  }))
                  .collect(collector.apply(executor))
                  .join();

                assertThat(new HashSet<>(result))
                  .hasSameSizeAs(elements)
                  .containsAll(elements);
            } finally {
                executor.shutdownNow();
            }
        });
    }
}
