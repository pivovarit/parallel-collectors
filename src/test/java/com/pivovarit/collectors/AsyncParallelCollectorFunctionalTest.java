package com.pivovarit.collectors;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToStream;
import static com.pivovarit.collectors.infrastructure.TestUtils.incrementAndThrow;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.infrastructure.TestUtils.runWithExecutor;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author Grzegorz Piwowarek
 */
class AsyncParallelCollectorFunctionalTest {

    private static final Executor executor = Executors.newFixedThreadPool(100);

    @TestFactory
    Stream<DynamicTest> testCollectors() {
        return of(
          forCollector((mapper, e) -> parallelToStream(mapper, e, 1000), "parallelToStream(p=1000)")
        ).flatMap(identity());
    }

    private static Stream<DynamicTest> forCollector(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, ?, CompletableFuture<Stream<Integer>>>> collector, String name) {
        return of(
          shouldCollect(collector, name),
          shouldCollectToEmpty(collector, name),
          shouldNotBlockWhenReturningFuture(collector, name),
          shouldShortCircuitOnException(collector, name),
          shouldNotSwallowException(collector, name),
          shouldInterruptOnException(collector, name),
          shouldSurviveRejectedExecutionException(collector, name),
          shouldRemainConsistent(collector, name),
          shouldStartConsumingImmediately(collector, name)
        );
    }

    private static <R extends Stream<Integer>> DynamicTest shouldNotBlockWhenReturningFuture(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should not block when returning future", name), () -> {
            List<Integer> elements = IntStream.of().boxed().collect(toList());
            assertTimeoutPreemptively(ofMillis(100), () ->
              elements.stream()
                .limit(5)
                .collect(collector
                  .apply(i -> returnWithDelay(42, ofMillis(Integer.MAX_VALUE)), executor)), "returned blocking future");
        });
    }

    private static <R extends Stream<Integer>> DynamicTest shouldCollectToEmpty(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should collect to empty", name), () -> {
            List<Integer> elements = IntStream.of().boxed().collect(toList());
            Collection<Integer> result11 = elements.stream().collect(collector.apply(i -> i, executor)).join().collect(Collectors.toList());

            assertThat(result11)
              .isEmpty();
        });
    }

    private static <R extends Stream<Integer>> DynamicTest shouldCollect(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should collect", name), () -> {
            List<Integer> elements = IntStream.range(0, 10).boxed().collect(toList());
            Collection<Integer> result = elements.stream().collect(collector.apply(i -> i, executor)).join().collect(Collectors.toList());

            assertThat(result)
              .hasSameSizeAs(elements)
              .containsOnlyElementsOf(elements);
        });
    }

    private static <R extends Stream<Integer>> DynamicTest shouldShortCircuitOnException(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should short circuit on exception", name), () -> {
            List<Integer> elements = IntStream.range(0, 100).boxed().collect(toList());
            int size = 4;

            runWithExecutor(e -> {
                // given
                LongAdder counter = new LongAdder();

                assertThatThrownBy(elements.stream()
                  .collect(collector.apply(i -> incrementAndThrow(counter), e))::join)
                  .isInstanceOf(CompletionException.class)
                  .hasCauseExactlyInstanceOf(IllegalArgumentException.class);

                assertThat(counter.longValue()).isLessThan(elements.size());
            }, size);
        });
    }

    private static <R extends Stream<Integer>> DynamicTest shouldNotSwallowException(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should not swallow exception", name), () -> {
            List<Integer> elements = IntStream.range(0, 10).boxed().collect(toList());

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

    private static <R extends Stream<Integer>> DynamicTest shouldSurviveRejectedExecutionException(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should not swallow exception", name), () -> {
            Executor executor = command -> { throw new RejectedExecutionException(); };
            List<Integer> elements = IntStream.range(0, 1000).boxed().collect(toList());

            assertThatThrownBy(() -> elements.stream()
              .collect(collector.apply(i -> returnWithDelay(i, ofMillis(10000)), executor))
              .join())
              .isInstanceOf(CompletionException.class)
              .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
        });
    }

    private static <R extends Stream<Integer>> DynamicTest shouldRemainConsistent(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should remain consistent", name), () -> {
            int parallelism = 100;

            ExecutorService executor = Executors.newFixedThreadPool(parallelism);
            try {
                List<Integer> elements = IntStream.range(0, parallelism).boxed().collect(toList());

                CountDownLatch countDownLatch = new CountDownLatch(parallelism);

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

                assertThat(new HashSet<>(result.collect(Collectors.toSet())))
                  .hasSameSizeAs(elements)
                  .containsAll(elements);
            } finally {
                executor.shutdownNow();
            }
        });
    }

    private static <R extends Stream<Integer>> DynamicTest shouldStartConsumingImmediately(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should start consuming immediately", name), () -> {
            AtomicInteger counter = new AtomicInteger();

            Stream.generate(() -> returnWithDelay(42, ofMillis(100))).limit(2)
              .collect(collector.apply(i -> counter.incrementAndGet(), executor));

            await()
              .atMost(150, TimeUnit.MILLISECONDS)
              .until(() -> counter.get() > 0);
        });
    }

    private static <R extends Stream<Integer>> DynamicTest shouldInterruptOnException(BiFunction<Function<Integer, Integer>, Executor, Collector<Integer, ?, CompletableFuture<Stream<Integer>>>> collector, String name) {
        return dynamicTest(format("%s: should interrupt on exception", name), () -> {
            AtomicLong counter = new AtomicLong();
            int size = 10;

            CountDownLatch countDownLatch = new CountDownLatch(size);

            runWithExecutor(e -> {
                assertThatThrownBy(IntStream.range(0, size).boxed()
                  .collect(collector.apply(i -> {
                      try {
                          countDownLatch.countDown();
                          countDownLatch.await();
                          Thread.sleep(50);
                          if (i == size - 1) throw new NullPointerException();
                          Thread.sleep(Integer.MAX_VALUE);
                      } catch (InterruptedException ex) {
                          counter.incrementAndGet();
                      }
                      return i;
                  }, e))::join)
                  .hasCauseExactlyInstanceOf(NullPointerException.class);

                await().until(() -> counter.get() == size - 1);
            }, size);
        });
    }
}
