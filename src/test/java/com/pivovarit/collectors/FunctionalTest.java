package com.pivovarit.collectors;

import com.pivovarit.collectors.infrastructure.TestUtils;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
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
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallel;
import static com.pivovarit.collectors.infrastructure.TestUtils.incrementAndThrow;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.infrastructure.TestUtils.runWithExecutor;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author Grzegorz Piwowarek
 */
class FunctionalTest {

    private static final int PARALLELISM = 1000;
    private static final Executor executor = Executors.newFixedThreadPool(100);

    @TestFactory
    Stream<DynamicTest> collectors() {
        return of(
          forCollector((mapper, e, p) -> parallel(mapper, toList(), e, p), format("parallel(toList(), p=%d)", PARALLELISM)),
          forCollector((mapper, e, p) -> parallel(mapper, toSet(), e, p), format("parallel(toSet(), p=%d)", PARALLELISM)),
          forCollector((mapper, e, p) -> parallel(mapper, toCollection(LinkedList::new), e, p), format("parallel(toCollection(), p=%d)", PARALLELISM)),
          forCollector((mapper, e, p) -> adapt(parallel(mapper, e, p)), format("parallel(p=%d)", PARALLELISM))
        ).flatMap(identity());
    }

    private static <R extends Collection<Integer>> Stream<DynamicTest> forCollector(CollectorSupplier<Function<Integer, Integer>, Executor, Integer, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return of(
          shouldCollect(collector, name),
          shouldCollectToEmpty(collector, name),
          shouldNotBlockWhenReturningFuture(collector, name),
          shouldShortCircuitOnException(collector, name),
          shouldInterruptOnException(collector, name),
          shouldRespectParallelism(collector, name),
          shouldNotSwallowException(collector, name),
          shouldSurviveRejectedExecutionException(collector, name),
          shouldRemainConsistent(collector, name),
          shouldStartConsumingImmediately(collector, name)
        );
    }

    private static <R extends Collection<Integer>> DynamicTest shouldNotBlockWhenReturningFuture(CollectorSupplier<Function<Integer, Integer>, Executor, Integer, Collector<Integer, ?, CompletableFuture<R>>> c, String name) {
        return dynamicTest(format("%s: should not block when returning future", name), () -> {
            assertTimeoutPreemptively(ofMillis(100), () ->
              Stream.<Integer>empty().collect(c.apply(i -> returnWithDelay(42, ofMillis(Integer.MAX_VALUE)), executor, PARALLELISM)), "returned blocking future");
        });
    }


    private static <R extends Collection<Integer>> DynamicTest shouldCollectToEmpty(CollectorSupplier<Function<Integer, Integer>, Executor, Integer, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should collect to empty", name), () -> {
            assertThat(Stream.<Integer>empty().collect(collector.apply(i -> i, executor, PARALLELISM)).join()).isEmpty();
        });
    }

    private static <R extends Collection<Integer>> DynamicTest shouldRespectParallelism(CollectorSupplier<Function<Integer, Integer>, Executor, Integer, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should respect parallelism", name), () -> {
            int parallelism = 2;
            TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();

            CompletableFuture<R> result = Stream.generate(() -> 42)
              .limit(10)
              .collect(collector.apply(i -> i, executor, parallelism));

            assertThat(result)
              .isNotCompleted()
              .isNotCancelled();

            await()
              .pollDelay(500, TimeUnit.MILLISECONDS)
              .until(() -> executor.count() == parallelism);
        });
    }


    private static <R extends Collection<Integer>> DynamicTest shouldCollect(CollectorSupplier<Function<Integer, Integer>, Executor, Integer, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should collect", name), () -> {
            List<Integer> elements = IntStream.range(0, 10).boxed().collect(toList());
            Collection<Integer> result = elements.stream().collect(collector.apply(i -> i, executor, PARALLELISM)).join();

            assertThat(result)
              .hasSameSizeAs(elements)
              .containsOnlyElementsOf(elements);
        });
    }


    private static <R extends Collection<Integer>> DynamicTest shouldShortCircuitOnException(CollectorSupplier<Function<Integer, Integer>, Executor, Integer, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should short circuit on exception", name), () -> {
            List<Integer> elements = IntStream.range(0, 100).boxed().collect(toList());
            int size = 4;

            runWithExecutor(e -> {
                LongAdder counter = new LongAdder();

                assertThatThrownBy(elements.stream()
                  .collect(collector.apply(i -> incrementAndThrow(counter), e, PARALLELISM))::join)
                  .isInstanceOf(CompletionException.class)
                  .hasCauseExactlyInstanceOf(IllegalArgumentException.class);

                assertThat(counter.longValue()).isLessThan(elements.size());
            }, size);
        });
    }


    private static <R extends Collection<Integer>> DynamicTest shouldNotSwallowException(CollectorSupplier<Function<Integer, Integer>, Executor, Integer, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
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
                  }, e, PARALLELISM))::join)
                  .isInstanceOf(CompletionException.class)
                  .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
            }, 10);
        });
    }


    private static <R extends Collection<Integer>> DynamicTest shouldSurviveRejectedExecutionException(CollectorSupplier<Function<Integer, Integer>, Executor, Integer, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should not swallow exception", name), () -> {
            Executor executor = command -> { throw new RejectedExecutionException(); };
            List<Integer> elements = IntStream.range(0, 1000).boxed().collect(toList());

            assertThatThrownBy(() -> elements.stream()
              .collect(collector.apply(i -> returnWithDelay(i, ofMillis(10000)), executor, PARALLELISM))
              .join())
              .isInstanceOf(CompletionException.class)
              .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
        });
    }


    private static <R extends Collection<Integer>> DynamicTest shouldRemainConsistent(CollectorSupplier<Function<Integer, Integer>, Executor, Integer, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
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
                  }, executor, PARALLELISM))
                  .join();

                assertThat(new HashSet<>(result))
                  .hasSameSizeAs(elements)
                  .containsAll(elements);
            } finally {
                executor.shutdownNow();
            }
        });
    }


    private static <R extends Collection<Integer>> DynamicTest shouldStartConsumingImmediately(CollectorSupplier<Function<Integer, Integer>, Executor, Integer, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should start consuming immediately", name), () -> {
            AtomicInteger counter = new AtomicInteger();

            Stream.generate(() -> returnWithDelay(42, ofMillis(100))).limit(2)
              .collect(collector.apply(i -> counter.incrementAndGet(), executor, PARALLELISM));

            await()
              .atMost(150, TimeUnit.MILLISECONDS)
              .until(() -> counter.get() > 0);
        });
    }

    private static <R extends Collection<Integer>> DynamicTest shouldInterruptOnException(CollectorSupplier<Function<Integer, Integer>, Executor, Integer, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
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
                          if (i == size - 1) throw new NullPointerException();
                          Thread.sleep(Integer.MAX_VALUE);
                      } catch (InterruptedException ex) {
                          counter.incrementAndGet();
                      }
                      return i;
                  }, e, PARALLELISM))::join)
                  .hasCauseExactlyInstanceOf(NullPointerException.class);

                await().until(() -> counter.get() == size - 1);
            }, size);
        });
    }

    private static Collector<Integer, ?, CompletableFuture<Collection<Integer>>> adapt(Collector<Integer, ?, CompletableFuture<Stream<Integer>>> input) {
        return collectingAndThen(input, stream -> stream.thenApply(s -> s.collect(Collectors.toList())));
    }

    @FunctionalInterface
    interface CollectorSupplier<T1, T2, T3, R> {
        R apply(T1 t1, T2 t2, T3 t3);
    }
}
