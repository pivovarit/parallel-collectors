package com.pivovarit.collectors.test;

import com.pivovarit.collectors.ParallelCollectors;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.test.NonBlockingTest.CollectorDefinition.collector;
import static java.time.Duration.ofDays;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class NonBlockingTest {

    private static Stream<CollectorDefinition<Integer, Integer>> allAsync() {
        return Stream.of(
          collector("parallel()", f -> collectingAndThen(ParallelCollectors.parallel(f), c -> c.thenApply(Stream::toList))),
          collector("parallel(toList())", f -> ParallelCollectors.parallel(f, toList())),
          collector("parallel(toList(), e)", f -> ParallelCollectors.parallel(f, toList(), e())),
          collector("parallel(toList(), e, p=1)", f -> ParallelCollectors.parallel(f, toList(), e(), 1)),
          collector("parallel(toList(), e, p=2)", f -> ParallelCollectors.parallel(f, toList(), e(), 2)),
          collector("parallel(toList(), e, p=1) [batching]", f -> ParallelCollectors.Batching.parallel(f, toList(), e(), 1)),
          collector("parallel(toList(), e, p=2) [batching]", f -> ParallelCollectors.Batching.parallel(f, toList(), e(), 2))
        );
    }

    @TestFactory
    Stream<DynamicTest> shouldNotBlockTheCallingThread() {
        return allAsync()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              assertTimeoutPreemptively(Duration.ofMillis(100), () -> {
                  var __ = Stream.of(1, 2, 3, 4).collect(c.factory().collector(i -> returnWithDelay(i, ofDays(1))));
              });
          }));
    }

    protected record CollectorDefinition<T, R>(String name, CollectorFactory<T, R> factory) {
        static <T, R> CollectorDefinition<T, R> collector(String name, CollectorFactory<T, R> collector) {
            return new CollectorDefinition<>(name, collector);
        }
    }

    @FunctionalInterface
    private interface CollectorFactory<T, R> {
        Collector<T, ?, CompletableFuture<List<R>>> collector(Function<T, R> f);
    }

    private static Executor e() {
        return Executors.newCachedThreadPool();
    }
}
