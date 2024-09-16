package com.pivovarit.collectors.test;

import com.pivovarit.collectors.ParallelCollectors;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.pivovarit.collectors.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.test.StreamingTest.CollectorDefinition.collector;
import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

class StreamingTest {

    private static Stream<CollectorDefinition<Integer, Integer>> allStreaming() {
        return Stream.of(
          collector("parallelToStream()", (f) -> ParallelCollectors.parallelToStream(f)),
          collector("parallelToStream(e)", (f) -> ParallelCollectors.parallelToStream(f, e())),
          collector("parallelToStream(e, p)", (f) -> ParallelCollectors.parallelToStream(f, e(), p())),
          collector("parallelToOrderedStream()", (f) -> ParallelCollectors.parallelToOrderedStream(f)),
          collector("parallelToOrderedStream(e)", (f) -> ParallelCollectors.parallelToOrderedStream(f, e())),
          collector("parallelToOrderedStream(e, p)", (f) -> ParallelCollectors.parallelToOrderedStream(f, e(), p()))
        );
    }

    @TestFactory
    Stream<DynamicTest> shouldPushElementsAsSoonAsTheyAreReady() {
        return allStreaming()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var counter = new AtomicInteger();
              Thread.startVirtualThread(() -> {
                  Stream.concat(Stream.of(0), IntStream.range(1, 10).boxed())
                    .collect(c.factory().collector(i -> returnWithDelay(i, ofSeconds(i))))
                    .forEach(__ -> counter.incrementAndGet());
              });

              await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofMillis(100))
                .until(() -> counter.get() > 0);
          }));
    }

    protected record CollectorDefinition<T, R>(String name, CollectorFactory<T, R> factory) {
        static <T, R> CollectorDefinition<T, R> collector(String name, CollectorFactory<T, R> collector) {
            return new CollectorDefinition<>(name, collector);
        }
    }

    @FunctionalInterface
    private interface CollectorFactory<T, R> {
        Collector<T, ?, Stream<R>> collector(Function<T, R> f);
    }

    private static Executor e() {
        return Executors.newCachedThreadPool();
    }

    private static int p() {
        return 4;
    }
}