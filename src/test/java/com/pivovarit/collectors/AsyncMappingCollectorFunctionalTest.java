package com.pivovarit.collectors;

import com.pivovarit.collectors.infrastructure.TestUtils;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
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
import java.util.function.BiFunction;
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
class AsyncMappingCollectorFunctionalTest {

    private static final Executor executor = Executors.newFixedThreadPool(100);

    @TestFactory
    Stream<DynamicTest> testCollectors() {
        return of(
          forCollector((mapper, e) -> parallelToSet(mapper, e), "parallelToSet(p=inf)"),
          forCollector((mapper, e) -> parallelToSet(mapper, e, 1000), "parallelToSet(p=1000)"),
          forCollector((mapper, e) -> parallelToList(mapper, e), "parallelToList(p=inf)"),
          forCollector((mapper, e) -> parallelToList(mapper, e, 1000), "parallelToList(p=1000)"),
          forCollector((mapper, e) -> parallelToCollection(mapper, ArrayList::new, e), "parallelToCollection(p=inf)"),
          forCollector((mapper, e) -> parallelToCollection(mapper, ArrayList::new, e, 1000), "parallelToCollection(p=1000)")
        ).flatMap(identity());
    }

    private static <R extends Collection<Integer>> Stream<DynamicTest> forCollector(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, List<CompletableFuture<Integer>>, CompletableFuture<R>>> collector, String name) {
        return of(
          shouldCollect(collector, name),
          shouldCollectToEmpty(collector, name),
          shouldNotBlockWhenReturningFuture(collector, name),
          shouldShortCircuitOnException(collector, name),
          shouldNotSwallowException(collector, name),
          shouldSurviveRejectedExecutionException(collector, name),
          shouldBeConsistent(collector, name)
//          shouldStartConsumingImmediately(collector, name) TODO enable once implemented
        );
    }

    //@Test
    private static <R extends Collection<Integer>> DynamicTest shouldNotBlockWhenReturningFuture(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, List<CompletableFuture<Integer>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should not block when returning future", name), () -> {
            List<Integer> elements = IntStream.of().boxed().collect(Collectors.toList());
            assertTimeoutPreemptively(ofMillis(100), () ->
              elements.stream()
                .limit(5)
                .collect(collector
                  .apply(i -> returnWithDelay(42, ofMillis(Integer.MAX_VALUE)), executor)), "returned blocking future");
        });
    }

    //@Test
    private static <R extends Collection<Integer>> DynamicTest shouldCollectToEmpty(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, List<CompletableFuture<Integer>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should collect to empty", name), () -> {
            List<Integer> elements = IntStream.of().boxed().collect(Collectors.toList());
            Collection<Integer> result11 = elements.stream().collect(collector.apply(i -> i, executor)).join();

            assertThat(result11)
              .isEmpty();
        });
    }

    //@Test
    private static <R extends Collection<Integer>> DynamicTest shouldCollect(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, List<CompletableFuture<Integer>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should collect", name), () -> {
            List<Integer> elements = IntStream.range(0, 10).boxed().collect(Collectors.toList());
            Collection<Integer> result = elements.stream().collect(collector.apply(i -> i, executor)).join();

            assertThat(result)
              .hasSameSizeAs(elements)
              .containsOnlyElementsOf(elements);
        });
    }

    //@Test
    private static <R extends Collection<Integer>> DynamicTest shouldShortCircuitOnException(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, List<CompletableFuture<Integer>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should short circuit on exception", name), () -> {
            List<Integer> elements = IntStream.range(0, 100).boxed().collect(Collectors.toList());
            int size = 4;

            runWithExecutor(e -> {
                // given
                LongAdder counter = new LongAdder();

                assertThatThrownBy(elements.stream()
                  .collect(collector.apply(i -> incrementAndThrow(counter), e))::join)
                  .isInstanceOf(CompletionException.class)
                  .hasCauseExactlyInstanceOf(IllegalArgumentException.class);

                assertThat(counter.longValue()).isLessThanOrEqualTo(size);
            }, size);
        });
    }

    //@Test
    private static <R extends Collection<Integer>> DynamicTest shouldNotSwallowException(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, List<CompletableFuture<Integer>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should not swallow exception", name), () -> {
            List<Integer> elements = IntStream.range(0, 10).boxed().collect(Collectors.toList());

            runWithExecutor(e -> {
                assertThatThrownBy(elements.stream()
                  .collect(collector.apply(i -> {
                      if (i == 7) {
                          throw new IllegalArgumentException();
                      } else {
                          return i;
                      }
                  }, e))::join)
                  .isInstanceOf(CompletionException.class)
                  .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
            }, 10);
        });
    }

    //@Test
    private static <R extends Collection<Integer>> DynamicTest shouldSurviveRejectedExecutionException(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, List<CompletableFuture<Integer>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should not swallow exception", name), () -> {
            Executor executor = command -> { throw new RejectedExecutionException(); };
            List<Integer> elements = IntStream.range(0, 1000).boxed().collect(Collectors.toList());

            assertThatThrownBy(() -> elements.stream()
              .collect(collector.apply(i -> returnWithDelay(i, ofMillis(10000)), executor))
              .join())
              .isInstanceOf(CompletionException.class)
              .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
        });
    }

    //@Test
    private static <R extends Collection<Integer>> DynamicTest shouldBeConsistent(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, List<CompletableFuture<Integer>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should remain consistent", name), () -> {
            ExecutorService executor = Executors.newFixedThreadPool(1000);
            try {
                List<Integer> elements = IntStream.range(0, 1000).boxed().collect(Collectors.toList());

                CountDownLatch countDownLatch = new CountDownLatch(1000);

                R result = elements.stream()
                  .collect(collector.apply(i -> {
                      countDownLatch.countDown();
                      try {
                          countDownLatch.await();
                      } catch (InterruptedException e) {
                          throw new RuntimeException(e);
                      }
                      return i;
                  }, executor))
                  .join();

                assertThat(new HashSet<>(result))
                  .hasSameSizeAs(elements)
                  .containsAll(elements);
            } finally {
                executor.shutdownNow();
            }
        });
    }

    //@Test
    private static <R extends Collection<Integer>> DynamicTest shouldStartConsumingImmediately(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, List<CompletableFuture<Integer>>, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should start consuming immediately", name), () -> {
            TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();

            assertTimeoutPreemptively(Duration.ofMillis(200), () -> {
                Stream.generate(() -> returnWithDelay(42, Duration.ofMillis(10)))
                  .limit(100)
                  .collect(collector.apply(i -> i, executor));
                assertThat(executor.count()).isGreaterThan(0);
            }, "didn't start processing after evaluating the first element");
        });
    }
}
