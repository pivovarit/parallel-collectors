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

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static com.pivovarit.collectors.ParallelCollectors.parallelToStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class MemoryReclamationTest {

    @Test
    void shouldNotRetainConsumedResultsWhileStreaming() {
        var blocker = new CountDownLatch(1);
        var refs = new ConcurrentLinkedQueue<WeakReference<Object>>();
        try {
            var results = IntStream.rangeClosed(0, 20).boxed()
              .collect(parallelToStream(i -> {
                  if (i == 20) {
                      try {
                          blocker.await();
                      } catch (InterruptedException e) {
                          Thread.currentThread().interrupt();
                      }
                      return new Object();
                  }
                  var payload = new Object();
                  refs.add(new WeakReference<>(payload));
                  return payload;
              }, c -> c.parallelism(4)));

            results.limit(20).forEach(__ -> {});

            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                System.gc();
                assertThat(refs).allSatisfy(ref -> assertThat(ref.get()).isNull());
            });
        } finally {
            blocker.countDown();
        }
    }
}
