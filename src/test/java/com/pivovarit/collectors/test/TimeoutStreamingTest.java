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

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static com.pivovarit.collectors.ParallelCollectors.parallelToStream;
import static com.pivovarit.collectors.ParallelCollectors.parallelToStreamBy;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeoutStreamingTest {

    private static Integer hangIf(Integer i, int hangValue) {
        if (i == hangValue) {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        return i;
    }

    @Test
    void shouldTimeoutUnordered() {
        assertThatThrownBy(() -> Stream.of(1, 2)
          .collect(parallelToStream(i -> hangIf(i, 1), c -> c.parallelism(2).timeout(100, MILLISECONDS)))
          .toList())
          .isInstanceOf(CompletionException.class)
          .hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void shouldEmitCompletedElementBeforeTimeoutUnordered() {
        var iterator = Stream.of(1, 2)
          .collect(parallelToStream(i -> hangIf(i, 2), c -> c.parallelism(2).timeout(500, MILLISECONDS)))
          .iterator();

        assertThat(iterator.next()).isEqualTo(1);
        assertThatThrownBy(iterator::next)
          .isInstanceOf(CompletionException.class)
          .hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void shouldTimeoutOrdered() {
        assertThatThrownBy(() -> Stream.of(1, 2)
          .collect(parallelToStream(i -> hangIf(i, 1), c -> c.parallelism(2).ordered().timeout(200, MILLISECONDS)))
          .toList())
          .isInstanceOf(CompletionException.class)
          .cause().isInstanceOf(TimeoutException.class).hasNoCause();
    }

    @Test
    void shouldTimeoutWhenBatching() {
        assertThatThrownBy(() -> Stream.of(1, 2, 3, 4)
          .collect(parallelToStream(i -> hangIf(i, 3), c -> c.parallelism(2).batching().timeout(200, MILLISECONDS)))
          .toList())
          .isInstanceOf(CompletionException.class)
          .hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void shouldNotTimeoutUnorderedWhenFastEnough() {
        var result = Stream.of(1, 2, 3)
          .collect(parallelToStream(i -> i + 1, c -> c.parallelism(2).timeout(10, SECONDS)))
          .toList();

        assertThat(result).containsExactlyInAnyOrder(2, 3, 4);
    }

    @Test
    void shouldNotTimeoutOrderedWhenFastEnough() {
        var result = Stream.of(1, 2, 3)
          .collect(parallelToStream(i -> i + 1, c -> c.parallelism(2).ordered().timeout(10, SECONDS)))
          .toList();

        assertThat(result).containsExactly(2, 3, 4);
    }

    @Test
    void shouldIgnoreTimeoutAtParallelismOne() {
        var result = Stream.of(1, 2, 3)
          .collect(parallelToStream(i -> i + 1, c -> c.parallelism(1).timeout(1, MILLISECONDS)))
          .toList();

        assertThat(result).containsExactly(2, 3, 4);
    }

    @Test
    void shouldEmitCompletedElementBeforeTimeoutOrdered() {
        var iterator = Stream.of(1, 2)
          .collect(parallelToStream(i -> hangIf(i, 2), c -> c.parallelism(2).ordered().timeout(500, MILLISECONDS)))
          .iterator();

        assertThat(iterator.next()).isEqualTo(1);
        assertThatThrownBy(iterator::next)
          .isInstanceOf(CompletionException.class)
          .hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void shouldTimeoutStreamingBy() {
        assertThatThrownBy(() -> Stream.of(1, 2, 3, 4)
          .collect(parallelToStreamBy(i -> i % 2, i -> hangIf(i, 3), c -> c.parallelism(2).timeout(200, MILLISECONDS)))
          .toList())
          .isInstanceOf(CompletionException.class)
          .hasCauseInstanceOf(TimeoutException.class);
    }
}
