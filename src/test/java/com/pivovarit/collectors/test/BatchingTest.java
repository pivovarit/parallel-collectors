package com.pivovarit.collectors.test;

import com.pivovarit.collectors.ParallelCollectors;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.test.BatchingTest.CollectorDefinition.collector;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class BatchingTest {
    private static Stream<BatchingTest.CollectorDefinition<Integer, Integer>> allBatching() {
        return Stream.of(
          collector("parallel(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallel(f, e(), p), c -> c.thenApply(Stream::toList).join())),
          collector("parallel(toList(), e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e(), p), CompletableFuture::join)),
          collector("parallelToStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e(), p), Stream::toList)),
          collector("parallelToOrderedStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p), Stream::toList))
        );
    }

    @TestFactory
    Stream<DynamicTest> shouldProcessOnExactlyNThreads() {
        return allBatching()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var threads = new ConcurrentSkipListSet<>();
              var parallelism = 4;

              Stream.generate(() -> 42)
                .limit(100)
                .collect(c.collector().collector(i -> {
                    threads.add(Thread.currentThread().getName());
                    return i;
                }, parallelism));

              assertThat(threads).hasSizeLessThanOrEqualTo(parallelism);
          }));
    }

    record CollectorDefinition<T, R>(String name, Factory.CollectorFactoryWithParallelism<T, R> collector) {
        static <T, R> CollectorDefinition<T, R> collector(String name, Factory.CollectorFactoryWithParallelism<T, R> collector) {
            return new CollectorDefinition<>(name, collector);
        }
    }

    private static Executor e() {
        return Executors.newCachedThreadPool();
    }
}
