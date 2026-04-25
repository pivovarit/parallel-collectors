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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class CancellationTest {

    @TestFactory
    Stream<DynamicTest> shouldInterruptRunningTasksOnCancellation() {
        return allCancellableCollectors()
          .map(c -> dynamicTest(c.name(), () -> {
              var executor = Executors.newVirtualThreadPerTaskExecutor();
              try {
                  var started = new CountDownLatch(c.minimumInterruptions());
                  var interrupted = new AtomicInteger();

                  var result = IntStream.range(0, c.taskCount())
                    .boxed()
                    .collect(c.factory().collector(i -> sleepUntilInterrupted(started, interrupted, i), executor));

                  assertThat(started.await(1, SECONDS)).isTrue();
                  assertThat(result.cancel(true)).isTrue();

                  await().atMost(5, SECONDS).untilAsserted(() -> assertThat(interrupted.get()).isGreaterThanOrEqualTo(c.minimumInterruptions()));
              } finally {
                  executor.shutdownNow();
              }
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldInterruptRunningTasksOnTimeout() {
        return allCancellableCollectors()
          .map(c -> dynamicTest(c.name(), () -> {
              var executor = Executors.newVirtualThreadPerTaskExecutor();
              try {
                  var started = new CountDownLatch(c.minimumInterruptions());
                  var interrupted = new AtomicInteger();

                  var result = IntStream.range(0, c.taskCount())
                    .boxed()
                    .collect(c.factory().collector(i -> sleepUntilInterrupted(started, interrupted, i), executor));

                  assertThat(started.await(1, SECONDS)).isTrue();
                  result.orTimeout(10, MILLISECONDS);

                  assertThatThrownBy(result::join)
                    .isInstanceOf(CompletionException.class)
                    .hasCauseExactlyInstanceOf(TimeoutException.class);
                  await().atMost(5, SECONDS).untilAsserted(() -> assertThat(interrupted.get()).isGreaterThanOrEqualTo(c.minimumInterruptions()));
              } finally {
                  executor.shutdownNow();
              }
          }));
    }

    private static Stream<CancellableCollector> allCancellableCollectors() {
        return Stream.of(
          collector("parallel() [executor]", 8, (f, e) -> ParallelCollectors.parallel(f, c -> c.executor(e))),
          collector("parallel() [executor, parallelism]", 4, (f, e) -> ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(4))),
          collector("parallel() [executor, parallelism, batching]", 4, (f, e) -> ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(4).batching())),
          collector("parallel() [executor, parallelism=1]", 1, (f, e) -> ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(1))),
          collector("parallel(toList()) [executor]", 8, (f, e) -> ParallelCollectors.parallel(f, c -> c.executor(e), toList())),
          collector("parallel(toList()) [executor, parallelism]", 4, (f, e) -> ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(4), toList())),
          collector("parallel(toList()) [executor, parallelism, batching]", 4, (f, e) -> ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(4).batching(), toList())),
          collector("parallel(toList()) [executor, parallelism=1]", 1, (f, e) -> ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(1), toList())),
          collector("parallelBy() [executor]", 8, (f, e) -> ParallelCollectors.parallelBy(i -> i, f, c -> c.executor(e))),
          collector("parallelBy() [executor, parallelism]", 4, (f, e) -> ParallelCollectors.parallelBy(i -> i, f, c -> c.executor(e).parallelism(4))),
          collector("parallelBy() [executor, parallelism, batching]", 4, (f, e) -> ParallelCollectors.parallelBy(i -> i, f, c -> c.executor(e).parallelism(4).batching())),
          collector("parallelBy() [executor, parallelism=1]", 1, (f, e) -> ParallelCollectors.parallelBy(i -> i, f, c -> c.executor(e).parallelism(1))),
          collector("parallelBy(toList()) [executor]", 8, (f, e) -> ParallelCollectors.parallelBy(i -> i, f, c -> c.executor(e), toList())),
          collector("parallelBy(toList()) [executor, parallelism]", 4, (f, e) -> ParallelCollectors.parallelBy(i -> i, f, c -> c.executor(e).parallelism(4), toList())),
          collector("parallelBy(toList()) [executor, parallelism, batching]", 4, (f, e) -> ParallelCollectors.parallelBy(i -> i, f, c -> c.executor(e).parallelism(4).batching(), toList())),
          collector("parallelBy(toList()) [executor, parallelism=1]", 1, (f, e) -> ParallelCollectors.parallelBy(i -> i, f, c -> c.executor(e).parallelism(1), toList()))
        );
    }

    private static CancellableCollector collector(String name, int minimumInterruptions, CancellableCollectorFactory factory) {
        return new CancellableCollector(name, 8, minimumInterruptions, factory);
    }

    private static int sleepUntilInterrupted(CountDownLatch started, AtomicInteger interrupted, int value) {
        started.countDown();
        try {
            Thread.sleep(Duration.ofDays(1));
        } catch (InterruptedException e) {
            interrupted.incrementAndGet();
            Thread.currentThread().interrupt();
        }
        return value;
    }

    private record CancellableCollector(String name, int taskCount, int minimumInterruptions, CancellableCollectorFactory factory) {
    }

    @FunctionalInterface
    private interface CancellableCollectorFactory {
        Collector<Integer, ?, ? extends CompletableFuture<?>> collector(Function<Integer, Integer> mapper, Executor executor);
    }
}
