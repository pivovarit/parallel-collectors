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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigProcessorTest {

    @Test
    void shouldProcessEmptyOptions() {
        var config = ConfigProcessor.process();

        assertThat(config.ordered()).isFalse();
        assertThat(config.batching()).isFalse();
        assertThat(config.parallelism()).isEqualTo(0);
        assertThat(config.executor()).isNotNull();
    }

    @Test
    void shouldProcessParallelismOption() {
        var config = ConfigProcessor.process(Options.parallelism(5));

        assertThat(config.parallelism()).isEqualTo(5);
        assertThat(config.ordered()).isFalse();
        assertThat(config.batching()).isFalse();
        assertThat(config.executor()).isNotNull();
    }

    @Test
    void shouldProcessExecutorOption() {
        Executor customExecutor = Executors.newSingleThreadExecutor();
        var config = ConfigProcessor.process(Options.executor(customExecutor));

        assertThat(config.executor()).isSameAs(customExecutor);
        assertThat(config.parallelism()).isEqualTo(0);
        assertThat(config.ordered()).isFalse();
        assertThat(config.batching()).isFalse();
    }

    @Test
    void shouldProcessOrderedOption() {
        var config = ConfigProcessor.process(Options.ordered());

        assertThat(config.ordered()).isTrue();
        assertThat(config.batching()).isFalse();
        assertThat(config.parallelism()).isEqualTo(0);
        assertThat(config.executor()).isNotNull();
    }

    @Test
    void shouldProcessBatchedOption() {
        var config = ConfigProcessor.process(Options.batched());

        assertThat(config.batching()).isTrue();
        assertThat(config.ordered()).isFalse();
        assertThat(config.parallelism()).isEqualTo(0);
        assertThat(config.executor()).isNotNull();
    }

    @Test
    void shouldProcessMultipleOptions() {
        Executor customExecutor = Executors.newFixedThreadPool(2);
        var config = ConfigProcessor.process(
          Options.parallelism(10),
          Options.executor(customExecutor),
          Options.ordered(),
          Options.batched()
        );

        assertThat(config.parallelism()).isEqualTo(10);
        assertThat(config.executor()).isSameAs(customExecutor);
        assertThat(config.ordered()).isTrue();
        assertThat(config.batching()).isTrue();
    }

    @Test
    void shouldRejectNullOptions() {
        assertThatThrownBy(() -> ConfigProcessor.process((Options.CollectingOption[]) null))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("options can't be null");
    }

    @Test
    void shouldRejectDuplicateParallelismOption() {
        assertThatThrownBy(() -> ConfigProcessor.process(
          Options.parallelism(5),
          Options.parallelism(10)
        ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("parallelism")
          .hasMessageContaining("multiple times");
    }

    @Test
    void shouldRejectDuplicateExecutorOption() {
        Executor executor1 = Executors.newSingleThreadExecutor();
        Executor executor2 = Executors.newSingleThreadExecutor();

        assertThatThrownBy(() -> ConfigProcessor.process(
          Options.executor(executor1),
          Options.executor(executor2)
        ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("executor")
          .hasMessageContaining("multiple times");
    }

    @Test
    void shouldRejectDuplicateOrderedOption() {
        assertThatThrownBy(() -> ConfigProcessor.process(
          Options.ordered(),
          Options.ordered()
        ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ordered")
          .hasMessageContaining("multiple times");
    }

    @Test
    void shouldRejectDuplicateBatchedOption() {
        assertThatThrownBy(() -> ConfigProcessor.process(
          Options.batched(),
          Options.batched()
        ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("batching")
          .hasMessageContaining("multiple times");
    }

    @Test
    void shouldProvideDefaultExecutor() {
        var config = ConfigProcessor.process();

        assertThat(config.executor()).isNotNull();
        // Verify default executor is functional by executing a simple task
        var future = new java.util.concurrent.CompletableFuture<Integer>();
        config.executor().execute(() -> future.complete(42));
        assertThat(future.join()).isEqualTo(42);
    }

    @Test
    void shouldRespectOptionOrdering() {
        Executor executor1 = Executors.newSingleThreadExecutor();
        Executor executor2 = Executors.newFixedThreadPool(2);

        // Even though executor2 is provided last, duplicate detection should trigger
        assertThatThrownBy(() -> ConfigProcessor.process(
          Options.executor(executor1),
          Options.parallelism(5),
          Options.executor(executor2)
        ))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("executor");
    }
}
