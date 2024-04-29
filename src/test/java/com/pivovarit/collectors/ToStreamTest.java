package com.pivovarit.collectors;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToStream;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Stream.of;
import static org.awaitility.Awaitility.await;

class ToStreamTest {

    @TestFactory
    Stream<DynamicTest> shouldStartProcessingElementsTests() {
        return of(
          shouldStartProcessingElements(f -> ParallelCollectors.parallelToStream(f, Executors.newCachedThreadPool(), 2), "parallelToStream, parallelism: 2, os threads"),
          shouldStartProcessingElements(f -> ParallelCollectors.parallelToOrderedStream(f, Executors.newVirtualThreadPerTaskExecutor(), 2), "parallelToStream, parallelism: 2, vthreads"),
          shouldStartProcessingElements(f -> ParallelCollectors.parallelToOrderedStream(f, Executors.newCachedThreadPool(), 2), "parallelToOrderedStream, parallelism: 2, os threads"),
          shouldStartProcessingElements(f -> ParallelCollectors.parallelToOrderedStream(f, Executors.newVirtualThreadPerTaskExecutor(), 2), "parallelToOrderedStream, parallelism: 2, vthreads")
          );
    }

    private static DynamicTest shouldStartProcessingElements(Function<Function<Integer, Integer>, Collector<Integer, ?, Stream<Integer>>> collector, String name) {
        return DynamicTest.dynamicTest(name, () -> {
            var counter = new AtomicInteger();
            Thread.ofPlatform().start(() -> {
                Stream.iterate(0, i -> i + 1)
                  .limit(100)
                  .collect(collector.apply(i -> {
                      try {
                          Thread.sleep(100);
                      } catch (InterruptedException ex) {
                          Thread.currentThread().interrupt();
                      }
                      return i;
                  }))
                  .forEach(c -> counter.incrementAndGet());
            });
            await()
              .atMost(ofSeconds(1))
              .until(() -> counter.get() > 0);
        });
    }

    @Test
    void shouldStartProcessingElementsAsSoonAsTheyAreReady() {
        var e = Executors.newCachedThreadPool();
        var counter = new AtomicInteger();
        Thread.ofPlatform().start(() -> {
            Collector<Integer, ?, Stream<Integer>> collector = parallelToStream(i -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                return i;
            }, e, 2);
            Stream.iterate(0, i -> i + 1)
              .limit(100)
              .collect(collector)
              .forEach(c -> counter.incrementAndGet());
        });
        await()
          .atMost(ofSeconds(1))
          .until(() -> counter.get() > 0);
    }
}
