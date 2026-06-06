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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

class LifecycleTest {

    private volatile Dispatcher<Integer> dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new Dispatcher<>(Executors.newCachedThreadPool());
    }

    @Test
    void shouldTerminateCollectingDispatcher() {
        var result = Stream.of(1, 2, 3)
          .collect(new AsyncParallelCollector<>(i -> i, dispatcher, Stream::toList)).join();

        assertThat(result).containsExactly(1, 2, 3);
        await().until(dispatcher::wasShutdown);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void shouldTerminateStreamingDispatcher(boolean ordered) {
        var result = Stream.of(1, 2, 3)
          .collect(new AsyncParallelStreamingCollector<>(i -> i, dispatcher, ordered)).toList();

        if (ordered) {
            assertThat(result).containsExactly(1, 2, 3);
        } else {
            assertThat(result).containsExactlyInAnyOrder(1, 2, 3);
        }
        await().until(dispatcher::wasShutdown);
    }

    @Test
    void shouldTerminateDispatcherWhenUpstreamThrowsMidTraversal() {
        var threadHolder = new AtomicReference<Thread>();
        var dispatcher = new Dispatcher<Integer>(Executors.newCachedThreadPool(), 2, threadHolder::set);

        assertThatThrownBy(() -> Stream.of(1, 2, 3, 4, 5)
          .peek(i -> {
              if (i == 3) {
                  throw new IllegalStateException("boom");
              }
          })
          .collect(new AsyncParallelCollector<>(i -> i, dispatcher, Stream::toList)))
          .isInstanceOf(IllegalStateException.class);

        await().untilAsserted(() -> {
            System.gc();
            assertThat(threadHolder.get())
              .as("dispatcher thread must terminate after an aborted traversal")
              .matches(t -> t == null || !t.isAlive());
        });
    }

}
