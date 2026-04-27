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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Parallelism1StreamSemanticTest {

    @Test
    void shouldRunMapperOnConfiguredExecutorNotCallerThread() {
        var callerThread = Thread.currentThread();
        var mapperThreads = new CopyOnWriteArrayList<Thread>();

        Executor executor = Executors.newSingleThreadExecutor();

        CompletableFuture<Stream<Integer>> future = Stream.of(1, 2, 3)
          .collect(ParallelCollectors.parallel(i -> {
              mapperThreads.add(Thread.currentThread());
              return i;
          }, c -> c.executor(executor).parallelism(1)));

        future.join().toList();

        assertThat(mapperThreads)
          .isNotEmpty()
          .allSatisfy(t -> assertThat(t).isNotEqualTo(callerThread));
    }

    @Test
    void shouldWrapMapperExceptionInCompletionException() {
        Executor executor = Executors.newSingleThreadExecutor();

        CompletableFuture<Stream<Integer>> future = Stream.of(1, 2, 3)
          .collect(ParallelCollectors.parallel(i -> {
              if (i == 2) {
                  throw new IllegalArgumentException("boom");
              }
              return i;
          }, c -> c.executor(executor).parallelism(1)));

        assertThatThrownBy(() -> future.join().toList())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCompleteExceptionallyBeforeStreamConsumed() {
        Executor executor = Executors.newSingleThreadExecutor();

        CompletableFuture<Stream<Integer>> future = Stream.of(1, 2, 3)
          .collect(ParallelCollectors.parallel(i -> {
              throw new IllegalArgumentException("boom");
          }, c -> c.executor(executor).parallelism(1)));

        assertThat(future)
          .failsWithin(java.time.Duration.ofSeconds(5))
          .withThrowableThat()
          .havingCause()
          .isInstanceOf(IllegalArgumentException.class);
    }
}
