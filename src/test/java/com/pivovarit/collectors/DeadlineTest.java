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

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongArray;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeadlineTest {

    @Test
    void shouldNotStartUntilFirstRemainingCall() throws InterruptedException {
        var deadline = new Deadline(TimeUnit.SECONDS.toNanos(1));

        // 500ms exceeds the 100ms assertion slack: an eager-start Deadline would report ~500ms remaining and fail
        Thread.sleep(500);

        long remaining = deadline.remainingNanos();
        assertThat(remaining).isGreaterThan(TimeUnit.MILLISECONDS.toNanos(900));
    }

    @Test
    void shouldCountDownAfterStart() throws InterruptedException {
        var deadline = new Deadline(TimeUnit.SECONDS.toNanos(1));

        long first = deadline.remainingNanos();
        Thread.sleep(50);
        long second = deadline.remainingNanos();

        assertThat(second).isGreaterThan(0).isLessThan(first);
    }

    @Test
    void shouldNotReturnNegativeAfterDeadlinePassed() throws InterruptedException {
        var deadline = new Deadline(TimeUnit.MILLISECONDS.toNanos(10));

        deadline.remainingNanos();
        Thread.sleep(50);

        assertThat(deadline.remainingNanos()).isZero();
    }

    @Test
    void shouldNotObserveSpuriousTimeoutWhenCalledConcurrently() {
        int threads = 16;
        var deadline = new Deadline(TimeUnit.HOURS.toNanos(1));
        var results = new AtomicLongArray(threads);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var barrier = new CyclicBarrier(threads);
            for (int i = 0; i < threads; i++) {
                int idx = i;
                executor.submit(() -> {
                    barrier.await();
                    results.set(idx, deadline.remainingNanos());
                    return null;
                });
            }
        }

        for (int i = 0; i < threads; i++) {
            assertThat(results.get(i)).isGreaterThan(TimeUnit.MINUTES.toNanos(59));
        }
    }
}
