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
package com.pivovarit.collectors;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsyncCollectorTest {

    @Test
    void shouldProcessElementsAsynchronously() {
        Executor executor = Executors.newSingleThreadExecutor();
        Function<Integer, Integer> mapper = i -> i * 2;
        Function<Stream<Integer>, List<Integer>> processor = s -> s.collect(toList());

        AsyncCollector<Integer, Integer, List<Integer>> collector =
          new AsyncCollector<>(mapper, processor, executor);

        CompletableFuture<List<Integer>> result = Stream.of(1, 2, 3)
          .collect(collector);

        assertThat(result.join()).containsExactly(2, 4, 6);
    }

    @Test
    void shouldHandleEmptyStream() {
        Executor executor = Executors.newSingleThreadExecutor();
        Function<Integer, Integer> mapper = i -> i * 2;
        Function<Stream<Integer>, List<Integer>> processor = s -> s.collect(toList());

        AsyncCollector<Integer, Integer, List<Integer>> collector =
          new AsyncCollector<>(mapper, processor, executor);

        CompletableFuture<List<Integer>> result = Stream.<Integer>empty()
          .collect(collector);

        assertThat(result.join()).isEmpty();
    }

    @Test
    void shouldPropagateExceptionFromMapper() {
        Executor executor = Executors.newSingleThreadExecutor();
        Function<Integer, Integer> mapper = i -> {
            if (i == 2) {
                throw new RuntimeException("Mapper failed");
            }
            return i * 2;
        };
        Function<Stream<Integer>, List<Integer>> processor = s -> s.collect(toList());

        AsyncCollector<Integer, Integer, List<Integer>> collector =
          new AsyncCollector<>(mapper, processor, executor);

        CompletableFuture<List<Integer>> result = Stream.of(1, 2, 3)
          .collect(collector);

        assertThatThrownBy(result::join)
          .hasCauseInstanceOf(RuntimeException.class)
          .hasMessageContaining("Mapper failed");
    }

    @Test
    void shouldExecuteOnProvidedExecutor() {
        AtomicInteger executionCount = new AtomicInteger(0);
        Executor trackingExecutor = task -> {
            executionCount.incrementAndGet();
            task.run();
        };

        Function<Integer, Integer> mapper = i -> i * 2;
        Function<Stream<Integer>, List<Integer>> processor = s -> s.collect(toList());

        AsyncCollector<Integer, Integer, List<Integer>> collector =
          new AsyncCollector<>(mapper, processor, trackingExecutor);

        Stream.of(1, 2, 3).collect(collector).join();

        // Should execute tasks on the provided executor
        assertThat(executionCount.get()).isGreaterThan(0);
    }

    @Test
    void shouldPreserveStreamOrder() {
        Executor executor = Executors.newFixedThreadPool(4);
        Function<Integer, Integer> mapper = i -> {
            // Add some variability to execution time
            try {
                Thread.sleep((4 - i) * 10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return i;
        };
        Function<Stream<Integer>, List<Integer>> processor = s -> s.collect(toList());

        AsyncCollector<Integer, Integer, List<Integer>> collector =
          new AsyncCollector<>(mapper, processor, executor);

        CompletableFuture<List<Integer>> result = Stream.of(1, 2, 3, 4)
          .collect(collector);

        assertThat(result.join()).containsExactly(1, 2, 3, 4);
    }

    @Test
    void shouldApplyFinisherFunction() {
        Executor executor = Executors.newSingleThreadExecutor();
        Function<Integer, Integer> mapper = i -> i;
        Function<Stream<Integer>, Long> finisher = Stream::count;

        AsyncCollector<Integer, Integer, Long> collector =
          new AsyncCollector<>(mapper, finisher, executor);

        CompletableFuture<Long> result = Stream.of(1, 2, 3, 4, 5)
          .collect(collector);

        assertThat(result.join()).isEqualTo(5L);
    }

    @Test
    void shouldHandleSingleElement() {
        Executor executor = Executors.newSingleThreadExecutor();
        Function<Integer, Integer> mapper = i -> i * 2;
        Function<Stream<Integer>, List<Integer>> processor = s -> s.collect(toList());

        AsyncCollector<Integer, Integer, List<Integer>> collector =
          new AsyncCollector<>(mapper, processor, executor);

        CompletableFuture<List<Integer>> result = Stream.of(42)
          .collect(collector);

        assertThat(result.join()).containsExactly(84);
    }

    @Test
    void shouldHandleNullElements() {
        Executor executor = Executors.newSingleThreadExecutor();
        Function<Integer, Integer> mapper = i -> i == null ? null : i * 2;
        Function<Stream<Integer>, List<Integer>> processor = s -> s.collect(toList());

        AsyncCollector<Integer, Integer, List<Integer>> collector =
          new AsyncCollector<>(mapper, processor, executor);

        CompletableFuture<List<Integer>> result = Stream.of(1, null, 3)
          .collect(collector);

        assertThat(result.join()).containsExactly(2, null, 6);
    }
}
