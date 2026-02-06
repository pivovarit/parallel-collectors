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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class OptionTest {

    @Test
    void shouldThrowOnInvalidParallelism() {
        assertThatThrownBy(() -> new ConfigProcessor.Option.Parallelism(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowOnNullExecutor() {
        assertThatThrownBy(() -> new ConfigProcessor.Option.ThreadPool(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectExecutorWithDiscardPolicy() {
        try (var executor = new ThreadPoolExecutor(2, 4, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), new ThreadPoolExecutor.DiscardPolicy())) {
            assertThatThrownBy(() -> new ConfigProcessor.Option.ThreadPool(executor)).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Executor's RejectedExecutionHandler can't discard tasks");
        }
    }

    @Test
    void shouldRejectExecutorWithDiscardOldestPolicy() {
        try (var executor = new ThreadPoolExecutor(2, 4, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), new ThreadPoolExecutor.DiscardOldestPolicy())) {
            assertThatThrownBy(() -> new ConfigProcessor.Option.ThreadPool(executor)).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Executor's RejectedExecutionHandler can't discard tasks");
        }
    }

    @Test
    void shouldThrowOnDuplicateBatching() {
        var configurer = new CollectingConfigurer();
        configurer.batching();
        assertThatThrownBy(configurer::batching)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("'batching' can only be configured once");
    }

    @Test
    void shouldThrowOnDuplicateParallelism() {
        var configurer = new CollectingConfigurer();
        configurer.parallelism(1);
        assertThatThrownBy(() -> configurer.parallelism(2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("'parallelism' can only be configured once");
    }

    @Test
    void shouldThrowOnDuplicateExecutor() {
        var configurer = new CollectingConfigurer();
        configurer.executor(r -> {});
        assertThatThrownBy(() -> configurer.executor(r -> {}))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("'executor' can only be configured once");
    }

    @Test
    void shouldThrowOnDuplicateOrdered() {
        var configurer = new StreamingConfigurer();
        configurer.ordered();
        assertThatThrownBy(configurer::ordered)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("'ordered' can only be configured once");
    }

}
