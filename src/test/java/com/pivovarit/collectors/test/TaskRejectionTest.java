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

import com.pivovarit.collectors.TestUtils;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.test.Factory.allWithCustomExecutors;
import static com.pivovarit.collectors.test.Factory.allWithCustomExecutorsParallelismOne;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class TaskRejectionTest {

    @TestFactory
    Stream<DynamicTest> shouldRejectInvalidRejectedExecutionHandlerFactory() {
        return allWithCustomExecutors()
          .flatMap(c -> Stream.of(new ThreadPoolExecutor.DiscardOldestPolicy(), new ThreadPoolExecutor.DiscardPolicy())
            .map(dp -> DynamicTest.dynamicTest("%s : %s".formatted(c.name(), dp.getClass().getSimpleName()), () -> {
                try (var e = new ThreadPoolExecutor(2, 2000, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), dp)) {
                    assertThatThrownBy(() -> Stream.of(1, 2, 3).collect(c.factory().collector(i -> i, e))).isExactlyInstanceOf(IllegalArgumentException.class);
                }
            })));
    }

    @TestFactory
    Stream<DynamicTest> shouldHandleRejection() {
        return allWithCustomExecutors()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              assertThatThrownBy(() -> {
                  try (var e = new ThreadPoolExecutor(2, 2, 0L, MILLISECONDS,
                    new LinkedBlockingQueue<>(1), new ThreadPoolExecutor.AbortPolicy())) {
                      assertTimeoutPreemptively(ofMillis(100), () -> of(1, 2, 3, 4)
                        .collect(c.factory().collector(i -> TestUtils.sleepAndReturn(1_000, i), e)));
                  }
              }).isExactlyInstanceOf(CompletionException.class);
          }));
    }
    @TestFactory
    Stream<DynamicTest> shouldHandleRejectionWhenParallelismOneFactory() {
        return allWithCustomExecutorsParallelismOne()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var e = new ThreadPoolExecutor(1, 1, 0L, SECONDS, new LinkedBlockingQueue<>(1));
              e.submit(() -> TestUtils.sleepAndReturn(10_000, 42));
              e.submit(() -> TestUtils.sleepAndReturn(10_000, 42));
              assertThatThrownBy(() -> {
                  assertTimeoutPreemptively(ofMillis(100), () -> of(1, 2, 3, 4)
                    .collect(c.factory().collector(i -> TestUtils.sleepAndReturn(1_000, i), e)));
              }).isExactlyInstanceOf(CompletionException.class);
          }));
    }
}
