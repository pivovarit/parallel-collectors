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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskDecoratorTest {

    @Test
    void shouldInterceptTasksViaDecoratorOnCollecting() {
        var intercepted = new AtomicInteger();

        var result = Stream.of(1, 2, 3)
          .collect(ParallelCollectors.parallel(i -> i, c -> c
            .taskDecorator(task -> () -> {
                intercepted.incrementAndGet();
                task.run();
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
            .taskDecorator(task -> () -> {
                intercepted.incrementAndGet();
                task.run();
            })))
          .toList();

        assertThat(result).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(intercepted.get()).isEqualTo(3);
    }

    @Test
    void shouldApplyDecoratorWithParallelism() {
        var intercepted = new AtomicInteger();

        var result = Stream.of(1, 2, 3, 4, 5)
          .collect(ParallelCollectors.parallelToStream(i -> i, c -> c
            .parallelism(2)
            .taskDecorator(task -> () -> {
                intercepted.incrementAndGet();
                task.run();
            })))
          .toList();

        assertThat(result).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
        assertThat(intercepted.get()).isEqualTo(5);
    }

    @Test
    void shouldApplyDecoratorOnOrderedStreaming() {
        var intercepted = new AtomicInteger();
        var source = List.of(1, 2, 3, 4, 5);

        var result = source.stream()
          .collect(ParallelCollectors.parallelToStream(i -> i, c -> c
            .ordered()
            .taskDecorator(task -> () -> {
                intercepted.incrementAndGet();
                task.run();
            })))
          .toList();

        assertThat(result).containsExactlyElementsOf(source);
        assertThat(intercepted.get()).isEqualTo(5);
    }

    @Test
    void shouldPropagateContextViaTaskDecorator() {
        var contextValue = new ThreadLocal<String>();
        var captured = new java.util.concurrent.CopyOnWriteArrayList<String>();

        contextValue.set("main-thread-value");
        var snapshot = contextValue.get();

        Stream.of(1, 2, 3)
          .collect(ParallelCollectors.parallel(i -> i, c -> c
            .taskDecorator(task -> () -> {
                contextValue.set(snapshot);
                try {
                    task.run();
                } finally {
                    captured.add(contextValue.get());
                    contextValue.remove();
                }
            }), toList()))
          .join();

        assertThat(captured).hasSize(3).containsOnly("main-thread-value");
    }

    @Test
    void shouldRejectNullTaskDecorator() {
        assertThatThrownBy(() ->
          Stream.of(1).collect(ParallelCollectors.parallel(i -> i, c -> c
            .taskDecorator(null), toList())))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectDuplicateTaskDecorator() {
        assertThatThrownBy(() ->
          Stream.of(1).collect(ParallelCollectors.parallel(i -> i, c -> c
            .taskDecorator(task -> task)
            .taskDecorator(task -> task), toList())))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("task decorator");
    }

    @Test
    void shouldCombineTaskDecoratorWithExecutorDecorator() {
        var executorDecoratorInvoked = new AtomicInteger();
        var taskDecoratorInvoked = new AtomicInteger();

        var result = Stream.of(1, 2, 3)
          .collect(ParallelCollectors.parallel(i -> i, c -> c
            .executorDecorator(exec -> task -> {
                executorDecoratorInvoked.incrementAndGet();
                exec.execute(task);
            })
            .taskDecorator(task -> () -> {
                taskDecoratorInvoked.incrementAndGet();
                task.run();
            }), toList()))
          .join();

        assertThat(result).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(executorDecoratorInvoked.get()).isEqualTo(3);
        assertThat(taskDecoratorInvoked.get()).isEqualTo(3);
    }
}
