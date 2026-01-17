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

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreconditionsTest {

    @Test
    void shouldAcceptValidParallelism() {
        assertThatCode(() -> Preconditions.requireValidParallelism(1))
          .doesNotThrowAnyException();

        assertThatCode(() -> Preconditions.requireValidParallelism(10))
          .doesNotThrowAnyException();

        assertThatCode(() -> Preconditions.requireValidParallelism(Integer.MAX_VALUE))
          .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidParallelism() {
        assertThatThrownBy(() -> Preconditions.requireValidParallelism(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Parallelism can't be lower than 1");

        assertThatThrownBy(() -> Preconditions.requireValidParallelism(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Parallelism can't be lower than 1");

        assertThatThrownBy(() -> Preconditions.requireValidParallelism(Integer.MIN_VALUE))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Parallelism can't be lower than 1");
    }

    @Test
    void shouldRejectNullExecutor() {
        assertThatThrownBy(() -> Preconditions.requireValidExecutor(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("Executor can't be null");
    }

    @Test
    void shouldAcceptValidExecutors() {
        assertThatCode(() -> Preconditions.requireValidExecutor(Runnable::run))
          .doesNotThrowAnyException();

        assertThatCode(() -> Preconditions.requireValidExecutor(Executors.newSingleThreadExecutor()))
          .doesNotThrowAnyException();

        assertThatCode(() -> Preconditions.requireValidExecutor(Executors.newFixedThreadPool(2)))
          .doesNotThrowAnyException();

        assertThatCode(() -> Preconditions.requireValidExecutor(
          new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadPoolExecutor.AbortPolicy())))
          .doesNotThrowAnyException();

        assertThatCode(() -> Preconditions.requireValidExecutor(
          new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadPoolExecutor.CallerRunsPolicy())))
          .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectThreadPoolExecutorWithDiscardPolicy() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
          1, 1, 0L, TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<>(),
          new ThreadPoolExecutor.DiscardPolicy());

        assertThatThrownBy(() -> Preconditions.requireValidExecutor(executor))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Executor's RejectedExecutionHandler can't discard tasks");
    }

    @Test
    void shouldRejectThreadPoolExecutorWithDiscardOldestPolicy() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
          1, 1, 0L, TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<>(),
          new ThreadPoolExecutor.DiscardOldestPolicy());

        assertThatThrownBy(() -> Preconditions.requireValidExecutor(executor))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Executor's RejectedExecutionHandler can't discard tasks");
    }
}
