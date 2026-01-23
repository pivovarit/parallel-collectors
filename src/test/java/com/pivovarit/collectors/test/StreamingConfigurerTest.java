/*
 * Copyright 2014-2026 Grzegorz Piwowarek, https://4comprehension.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pivovarit.collectors.test;

import com.pivovarit.collectors.ParallelCollectors;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.pivovarit.collectors.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.test.Factory.e;
import static com.pivovarit.collectors.test.Factory.p;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class StreamingConfigurerTest {

    static <T, R> Factory.GenericCollector<Factory.StreamingCollectorFactory<T, R>> streamingCollector(String name, Factory.StreamingCollectorFactory<T, R> collector) {
        return new Factory.GenericCollector<>(name, collector);
    }

    private static Stream<Factory.GenericCollector<Factory.StreamingCollectorFactory<Integer, Integer>>> allStreaming() {
        return Stream.of(
          streamingCollector("parallelToStream()", (f) -> ParallelCollectors.parallelToStream(f)),
          streamingCollector("parallelToStream(e)", (f) -> ParallelCollectors.parallelToStream(f, c -> c.executor(e()))),
          streamingCollector("parallelToStream(e, p)", (f) -> ParallelCollectors.parallelToStream(f, c -> c.executor(e()).parallelism(p()))),
          streamingCollector("parallelToOrderedStream()", (f) -> ParallelCollectors.parallelToStream(f, c -> c.ordered())),
          streamingCollector("parallelToOrderedStream(e)", (f) -> ParallelCollectors.parallelToStream(f, c -> c.ordered().executor(e()))),
          streamingCollector("parallelToOrderedStream(e, p)", (f) -> ParallelCollectors.parallelToStream(f, c -> c.ordered().executor(e()).parallelism(p())))
        );
    }

    private static Stream<Factory.GenericCollector<Factory.StreamingCollectorFactory<Integer, Integer>>> allCompletionOrderStreaming() {
        return Stream.of(
          streamingCollector("parallelToStream()", (f) -> ParallelCollectors.parallelToStream(f)),
          streamingCollector("parallelToStream(e)", (f) -> ParallelCollectors.parallelToStream(f, c -> c.executor(e()))),
          streamingCollector("parallelToStream(e, p)", (f) -> ParallelCollectors.parallelToStream(f, c -> c.executor(e()).parallelism(p())))
        );
    }

    private static Stream<Factory.GenericCollector<Factory.StreamingCollectorFactory<Integer, Integer>>> allOrderedStreaming() {
        return Stream.of(
          streamingCollector("parallelToOrderedStream()", (f) -> ParallelCollectors.parallelToStream(f, c -> c.ordered())),
          streamingCollector("parallelToOrderedStream(e)", (f) -> ParallelCollectors.parallelToStream(f, c -> c.ordered().executor(e()))),
          streamingCollector("parallelToOrderedStream(e, p)", (f) -> ParallelCollectors.parallelToStream(f, c -> c.ordered().executor(e()).parallelism(p())))
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

    @Test
    void shouldUseSyncFallback() {
        var result = Stream.of(1, 2, 3, 4)
          .collect(ParallelCollectors.parallelToStream(i -> i, c -> c.executor(r -> {
              throw new IllegalStateException("boo!");
          }).parallelism(1)))
          .toList();

        assertThat(result).containsExactly(1, 2, 3, 4);
    }

    @Test
    void shouldUseSyncFallbackForOrdered() {
        var result = Stream.of(1, 2, 3, 4)
          .collect(ParallelCollectors.parallelToStream(i -> i, c -> c.executor(r -> {
              throw new IllegalStateException("boo!");
          }).parallelism(1).ordered()))
          .toList();

        assertThat(result).containsExactly(1, 2, 3, 4);
    }
}
