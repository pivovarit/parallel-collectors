package com.pivovarit.collectors;

import com.pivovarit.collectors.ParallelCollectors.Batching;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallel;
import static com.pivovarit.collectors.ParallelCollectors.parallelToStream;
import static com.pivovarit.collectors.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.TestUtils.withExecutor;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author Grzegorz Piwowarek
 */
class FunctionalTest {

    private static final int PARALLELISM = 1000;

    @TestFactory
    Stream<DynamicTest> collectors() {
        return of(
          // platform threads, with batching
          batchTests((m, e, p) -> Batching.parallel(m, toList(), e, p), format("ParallelCollectors.Batching.parallel(toList(), p=%d)", PARALLELISM)),
          batchTests((m, e, p) -> Batching.parallel(m, toSet(), e, p), format("ParallelCollectors.Batching.parallel(toSet(), p=%d)", PARALLELISM)),
          batchTests((m, e, p) -> Batching.parallel(m, toCollection(LinkedList::new), e, p), format("ParallelCollectors.Batching.parallel(toCollection(), p=%d)", PARALLELISM)),
          batchTests((m, e, p) -> adapt(Batching.parallel(m, e, p)), format("ParallelCollectors.Batching.parallel(p=%d)", PARALLELISM))
        ).flatMap(i -> i);
    }

    @TestFactory
    Stream<DynamicTest> streaming_batching_collectors() {
        return of(
          // virtual threads
          batchStreamingTests((m, e, p) -> adaptAsync(Batching.parallelToStream(m, e, p)), "ParallelCollectors.Batching.parallelToStream() [virtual]"),
          batchStreamingTests((m, e, p) -> adaptAsync(Batching.parallelToOrderedStream(m, e, p)), "ParallelCollectors.Batching.parallelToOrderedStream(p=%d) [virtual]"),
          // platform threads
          batchStreamingTests((m, e, p) -> adaptAsync(Batching.parallelToStream(m, e, p)), format("ParallelCollectors.Batching.parallelToStream(p=%d)", PARALLELISM)),
          batchStreamingTests((m, e, p) -> adaptAsync(Batching.parallelToOrderedStream(m, e, p)), format("ParallelCollectors.Batching.parallelToOrderedStream(p=%d)", PARALLELISM))
        ).flatMap(i -> i);
    }

    @Test
    void shouldCollectInCompletionOrder() {
        // given
        try (var executor = threadPoolExecutor(4)) {
            List<Integer> result = of(350, 200, 0, 400)
              .collect(parallelToStream(i -> returnWithDelay(i, ofMillis(i)), executor, 4))
              .limit(2)
              .toList();

            assertThat(result).isSorted();
        }
    }

    @Test
    void shouldCollectEagerlyInCompletionOrder() {
        // given
        var executor = threadPoolExecutor(4);
        AtomicBoolean result = new AtomicBoolean(false);
        CompletableFuture.runAsync(() -> {
            of(1, 10000, 1, 0)
              .collect(parallelToStream(i -> returnWithDelay(i, ofMillis(i)), executor, 2))
              .forEach(i -> {
                  if (i == 1) {
                      result.set(true);
                  }
              });
        });

        await()
          .atMost(1, SECONDS)
          .until(result::get);
    }

    @Test
    void shouldExecuteEagerlyOnProvidedThreadPool() {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var countingExecutor = new CountingExecutor(executor);
            var executions = new AtomicInteger();
            var list = List.of("A", "B");

            list.stream()
              .collect(parallel(s -> {
                  executions.incrementAndGet();
                  return s;
              }, countingExecutor, 1))
              .join()
              .forEach(__ -> {});

            assertThat(countingExecutor.getInvocations()).isEqualTo(1);
            assertThat(executions.get()).isEqualTo(2);
        }
    }

    private static <R extends Collection<Integer>> Stream<DynamicTest> batchTests(CollectorSupplier<Function<Integer, Integer>, Executor, Integer, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return of(shouldProcessOnNThreadsETParallelism(collector, name));
    }

    private static <R extends Collection<Integer>> Stream<DynamicTest> batchStreamingTests(CollectorSupplier<Function<Integer, Integer>, Executor, Integer, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return of(shouldProcessOnNThreadsETParallelism(collector, name));
    }

    private static <R extends Collection<Integer>> DynamicTest shouldProcessOnNThreadsETParallelism(CollectorSupplier<Function<Integer, Integer>, Executor, Integer, Collector<Integer, ?, CompletableFuture<R>>> collector, String name) {
        return dynamicTest(format("%s: should batch", name), () -> {
            int parallelism = 2;

            withExecutor(e -> {
                Set<String> threads = new ConcurrentSkipListSet<>();

                Stream.generate(() -> 42)
                  .limit(100)
                  .collect(collector.apply(i -> {
                      threads.add(Thread.currentThread().getName());
                      return i;
                  }, e, parallelism))
                  .join();

                assertThat(threads).hasSize(parallelism);
            });
        });
    }

    private static Collector<Integer, ?, CompletableFuture<Collection<Integer>>> adapt(Collector<Integer, ?, CompletableFuture<Stream<Integer>>> input) {
        return collectingAndThen(input, stream -> stream.thenApply(Stream::toList));
    }

    private static Collector<Integer, ?, CompletableFuture<Collection<Integer>>> adaptAsync(Collector<Integer, ?, Stream<Integer>> input) {
        return collectingAndThen(input, stream -> CompletableFuture
          .supplyAsync(stream::toList, Executors.newSingleThreadExecutor()));
    }

    private static ThreadPoolExecutor threadPoolExecutor(int unitsOfWork) {
        return new ThreadPoolExecutor(unitsOfWork, unitsOfWork,
          0L, MILLISECONDS,
          new LinkedBlockingQueue<>());
    }

    @FunctionalInterface
    interface CollectorSupplier<T1, T2, T3, R> {
        R apply(T1 t1, T2 t2, T3 t3);
    }

    private static class CountingExecutor implements Executor {

        private final AtomicInteger counter = new AtomicInteger();

        private final Executor delegate;

        public CountingExecutor(Executor delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(Runnable command) {
            counter.incrementAndGet();
            delegate.execute(command);
        }

        public Integer getInvocations() {
            return counter.get();
        }
    }
}
