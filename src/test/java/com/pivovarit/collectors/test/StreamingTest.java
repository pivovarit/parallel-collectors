package com.pivovarit.collectors.test;

import static com.pivovarit.collectors.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.test.Factory.GenericCollector.streamingCollector;
import static com.pivovarit.collectors.test.Factory.e;
import static com.pivovarit.collectors.test.Factory.p;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.pivovarit.collectors.ParallelCollectors;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class StreamingTest {

    private static Stream<Factory.GenericCollector<Factory.StreamingCollectorFactory<Integer, Integer>>> allStreaming() {
        return Stream.of(
          streamingCollector("parallelToStream()", (f) -> ParallelCollectors.parallelToStream(f)),
          streamingCollector("parallelToStream(e)", (f) -> ParallelCollectors.parallelToStream(f, e())),
          streamingCollector("parallelToStream(e, p)", (f) -> ParallelCollectors.parallelToStream(f, e(), p())),
          streamingCollector("parallelToOrderedStream()", (f) -> ParallelCollectors.parallelToOrderedStream(f)),
          streamingCollector("parallelToOrderedStream(e)", (f) -> ParallelCollectors.parallelToOrderedStream(f, e())),
          streamingCollector("parallelToOrderedStream(e, p)", (f) -> ParallelCollectors.parallelToOrderedStream(f, e(), p()))
        );
    }

    private static Stream<Factory.GenericCollector<Factory.StreamingCollectorFactory<Integer, Integer>>> allCompletionOrderStreaming() {
        return Stream.of(
          streamingCollector("parallelToStream()", (f) -> ParallelCollectors.parallelToStream(f)),
          streamingCollector("parallelToStream(e)", (f) -> ParallelCollectors.parallelToStream(f, e())),
          streamingCollector("parallelToStream(e, p)", (f) -> ParallelCollectors.parallelToStream(f, e(), p()))
        );
    }

    private static Stream<Factory.GenericCollector<Factory.StreamingCollectorFactory<Integer, Integer>>> allOrderedStreaming() {
        return Stream.of(
          streamingCollector("parallelToOrderedStream()", (f) -> ParallelCollectors.parallelToOrderedStream(f)),
          streamingCollector("parallelToOrderedStream(e)", (f) -> ParallelCollectors.parallelToOrderedStream(f, e())),
          streamingCollector("parallelToOrderedStream(e, p)", (f) -> ParallelCollectors.parallelToOrderedStream(f, e(), p()))
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

    @TestFactory
    Stream<DynamicTest> shouldCollectInCompletionOrder() {
        return allCompletionOrderStreaming()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var result = of(300, 200, 0, 400)
                .collect(c.factory().collector(i -> returnWithDelay(i, ofMillis(i))))
                .toList();

              assertThat(result).isSorted();
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldCollectInOriginalOrder() {
        return allOrderedStreaming()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var source = List.of(300, 200, 0, 400);
              var result = source.stream()
                .collect(c.factory().collector(i -> returnWithDelay(i, ofMillis(i))))
                .toList();

              assertThat(result).containsExactlyElementsOf(source);
          }));
    }
}
