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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutorDecoratorTest {

    @Test
    void shouldInterceptTasksViaDecoratorOnCollecting() {
        var intercepted = new AtomicInteger();

        var result = Stream.of(1, 2, 3)
          .collect(ParallelCollectors.parallel(i -> i, c -> c
            .executorDecorator(exec -> task -> {
                intercepted.incrementAndGet();
                exec.execute(task);
            }), toList()))
          .join();

        assertThat(result).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(intercepted.get()).isEqualTo(3);
    }

    @Test
    void shouldInterceptTasksViaDecoratorOnStreaming() {
        var intercepted = new AtomicInteger();

        var result = Stream.of(1, 2, 3)
          .collect(ParallelCollectors.parallelToStream(i -> i, c -> c
            .executorDecorator(exec -> task -> {
                intercepted.incrementAndGet();
                exec.execute(task);
            })))
          .toList();

        assertThat(result).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(intercepted.get()).isEqualTo(3);
    }

    @Test
    void shouldDecorateCustomExecutor() {
        var decoratedExecutor = new AtomicReference<Object>();
        var customExecutor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));

        try (customExecutor) {
            Stream.of(1, 2, 3)
              .collect(ParallelCollectors.parallel(i -> i, c -> c
                .executor(customExecutor)
                .executorDecorator(exec -> {
                    decoratedExecutor.set(exec);
                    return exec;
                }), toList()))
              .join();
        }

        assertThat(decoratedExecutor.get()).isSameAs(customExecutor);
    }

    @Test
    void shouldDecorateDefaultExecutorWhenNoCustomExecutorProvided() {
        var decoratorInvoked = new AtomicInteger();

        Stream.of(1, 2, 3)
          .collect(ParallelCollectors.parallel(i -> i, c -> c
            .executorDecorator(exec -> {
                decoratorInvoked.incrementAndGet();
                return exec;
            }), toList()))
          .join();

        assertThat(decoratorInvoked.get()).isEqualTo(1);
    }

    @Test
    void shouldRejectDecoratorReturningDiscardingExecutor() {
        var discarding = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.DiscardPolicy());

        try (discarding) {
            assertThatThrownBy(() ->
              Stream.of(1).collect(ParallelCollectors.parallel(i -> i, c -> c
                .executorDecorator(ignored -> discarding), toList())))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("can't discard tasks");
        }
    }

    @Test
    void shouldRejectDecoratorReturningNull() {
        assertThatThrownBy(() ->
          Stream.of(1).collect(ParallelCollectors.parallel(i -> i, c -> c
            .executorDecorator(ignored -> null), toList())))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldApplyDecoratorOnStreamingWithParallelism() {
        var intercepted = new AtomicInteger();

        var result = Stream.of(1, 2, 3, 4, 5)
          .collect(ParallelCollectors.parallelToStream(i -> i, c -> c
            .parallelism(2)
            .executorDecorator(exec -> task -> {
                intercepted.incrementAndGet();
                exec.execute(task);
            })))
          .toList();

        assertThat(result).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
        assertThat(intercepted.get()).isEqualTo(5);
    }

    @Test
    void shouldApplyDecoratorOnOrderedStreaming() {
        var intercepted = new AtomicInteger();
        var source = java.util.List.of(1, 2, 3, 4, 5);

        var result = source.stream()
          .collect(ParallelCollectors.parallelToStream(i -> i, c -> c
            .ordered()
            .executorDecorator(exec -> task -> {
                intercepted.incrementAndGet();
                exec.execute(task);
            })))
          .toList();

        assertThat(result).containsExactlyElementsOf(source);
        assertThat(intercepted.get()).isEqualTo(5);
    }
}
