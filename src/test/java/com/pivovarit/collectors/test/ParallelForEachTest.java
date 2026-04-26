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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParallelForEachTest {

    @TestFactory
    Stream<DynamicTest> shouldProcessAllElements() {
        return allForEach()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var processed = new ConcurrentLinkedQueue<Integer>();
              var list = IntStream.range(0, 100).boxed().toList();

              list.stream()
                .collect(c.collector(processed::add))
                .join();

              assertThat(processed).containsExactlyInAnyOrderElementsOf(list);
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldProcessEmpty() {
        return allForEach()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var counter = new AtomicInteger();

              Stream.<Integer>empty()
                .collect(c.collector(i -> counter.incrementAndGet()))
                .join();

              assertThat(counter.get()).isZero();
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldPropagateException() {
        return allForEach()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              assertThatThrownBy(() -> IntStream.range(0, 10)
                .boxed()
                .collect(c.collector(i -> {
                    if (i == 7) {
                        throw new IllegalArgumentException();
                    }
                }))
                .join())
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldReturnCompletableFutureVoid() {
        return allForEach()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              CompletableFuture<Void> result = Stream.of(1, 2, 3)
                .collect(c.collector(i -> {}));

              assertThat(result.join()).isNull();
          }));
    }

    record ForEachCollector(String name, ForEachFactory factory) {
        Collector<Integer, ?, CompletableFuture<Void>> collector(Consumer<Integer> action) {
            return factory.create(action);
        }
    }

    @FunctionalInterface
    interface ForEachFactory {
        Collector<Integer, ?, CompletableFuture<Void>> create(Consumer<Integer> action);
    }

    private static Stream<ForEachCollector> allForEach() {
        var executor = Executors.newCachedThreadPool();
        return Stream.of(
          new ForEachCollector("parallelForEach()",
            ParallelCollectors::parallelForEach),
          new ForEachCollector("parallelForEach(c -> {})",
            a -> ParallelCollectors.parallelForEach(a, c -> {})),
          new ForEachCollector("parallelForEach(c -> c.parallelism(4))",
            a -> ParallelCollectors.parallelForEach(a, c -> c.parallelism(4))),
          new ForEachCollector("parallelForEach(c -> c.executor(e))",
            a -> ParallelCollectors.parallelForEach(a, c -> c.executor(executor))),
          new ForEachCollector("parallelForEach(c -> c.executor(e).parallelism(4))",
            a -> ParallelCollectors.parallelForEach(a, c -> c.executor(executor).parallelism(4))),
          new ForEachCollector("parallelForEach(c -> c.executor(e).parallelism(4).batching())",
            a -> ParallelCollectors.parallelForEach(a, c -> c.executor(executor).parallelism(4).batching())),
          new ForEachCollector("parallelForEach(4)",
            a -> ParallelCollectors.parallelForEach(a, 4)),
          new ForEachCollector("parallelForEach(1)",
            a -> ParallelCollectors.parallelForEach(a, 1))
        );
    }
}
