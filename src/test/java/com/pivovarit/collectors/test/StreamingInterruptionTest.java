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
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class StreamingInterruptionTest {

    @TestFactory
    Stream<DynamicTest> shouldUnblockConsumerOnInterrupt() {
        return allStreamingCollectors()
          .map(c -> dynamicTest(c.name(), () -> {
              var stream = Stream.of(1, 2)
                .collect(c.factory().apply(StreamingInterruptionTest::hangForever));

              var thrown = new AtomicReference<Throwable>();
              var interruptRestored = new AtomicBoolean();
              var consumer = new Thread(() -> {
                  try {
                      stream.forEach(__ -> {});
                  } catch (Throwable e) {
                      interruptRestored.set(Thread.currentThread().isInterrupted());
                      thrown.set(e);
                  }
              });
              consumer.start();

              await().atMost(5, SECONDS)
                .until(() -> consumer.getState() == Thread.State.WAITING || consumer.getState() == Thread.State.TIMED_WAITING);

              consumer.interrupt();
              consumer.join(5_000);

              assertThat(consumer.isAlive()).as("consumer should unblock on interrupt").isFalse();
              assertThat(thrown.get())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(InterruptedException.class);
              assertThat(interruptRestored).as("interrupt flag should be restored").isTrue();
          }));
    }

    private static Stream<StreamingCollector> allStreamingCollectors() {
        return Stream.of(
          new StreamingCollector("parallelToStream() [ordered]", f -> ParallelCollectors.parallelToStream(f, c -> c.parallelism(2).ordered())),
          new StreamingCollector("parallelToStream() [ordered, timeout]", f -> ParallelCollectors.parallelToStream(f, c -> c.parallelism(2).ordered().timeout(Duration.ofDays(1)))),
          new StreamingCollector("parallelToStream() [unordered]", f -> ParallelCollectors.parallelToStream(f, c -> c.parallelism(2))),
          new StreamingCollector("parallelToStream() [unordered, timeout]", f -> ParallelCollectors.parallelToStream(f, c -> c.parallelism(2).timeout(Duration.ofDays(1))))
        );
    }

    private static Integer hangForever(Integer i) {
        try {
            Thread.sleep(Duration.ofDays(1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return i;
    }

    private record StreamingCollector(String name, Function<Function<Integer, Integer>, Collector<Integer, ?, Stream<Integer>>> factory) {
    }
}
