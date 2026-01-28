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
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.test.Factory.allBatching;
import static java.time.Duration.ofMillis;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;

class BatchingTest {

    @TestFactory
    Stream<DynamicTest> shouldProcessOnExactlyNThreads() {
        return allBatching()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var threads = new ConcurrentSkipListSet<>();
              var parallelism = 4;

              var ignored = Stream.generate(() -> 42)
                .limit(100)
                .collect(c.factory().collector(i -> {
                    threads.add(Thread.currentThread().getName());
                    return i;
                }, parallelism));

              assertThat(threads).hasSizeLessThanOrEqualTo(parallelism);
          }));
    }

    @Test
    void shouldCollectInCompletionOrder() {
        var result = of(300, 200, 0, 400)
          .collect(ParallelCollectors.parallelToStream(i -> returnWithDelay(i, ofMillis(i)), c -> c.batching().executor(Executors.newVirtualThreadPerTaskExecutor()).parallelism(2)))
          .toList();

        assertThat(result).containsExactly(0, 400, 300, 200);
    }

    @Test
    void shouldCollectInOriginalOrder() {
        var result = of(300, 200, 0, 400)
          .collect(ParallelCollectors.parallelToStream(i -> returnWithDelay(i, ofMillis(i)), c -> c.ordered().batching().executor(Executors.newVirtualThreadPerTaskExecutor()).parallelism(2)))
          .toList();

        assertThat(result).containsExactly(300, 200, 0, 400);
    }
}
