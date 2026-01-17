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

import java.lang.reflect.Field;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class DispatcherTest {

    @Test
    void shouldShutdownExecutorOnStop() {
        AtomicReference<Thread> holder = new AtomicReference<>();

        var dispatcher = new Dispatcher<Integer>(Executors.newCachedThreadPool(), 1, holder::set);
        dispatcher.start();
        dispatcher.submit(() -> 42);
        dispatcher.stop();

        await().untilAsserted(() -> assertThat(holder.get().isAlive()).isFalse());
    }

    @Test
    void shouldNotAcquirePermitOnPoisonPill() throws Exception {
        var executorGate = new Semaphore(0);

        Executor blockingExecutor = command -> {
            try {
                executorGate.acquire();
                command.run();
            } catch (InterruptedException ignored) {
            }
        };

        var dispatcher = new Dispatcher<>(blockingExecutor, 1);

        dispatcher.start();
        dispatcher.stop();

        Semaphore limiter = extractSemaphore(dispatcher);

        assertThat(limiter.tryAcquire(100, TimeUnit.MILLISECONDS))
          .as("no permit should be leaked on stop")
          .isTrue();
    }

    private static Semaphore extractSemaphore(Dispatcher<?> dispatcher) throws Exception {
        Field limiterField = Dispatcher.class.getDeclaredField("limiter");
        limiterField.setAccessible(true);
        return (Semaphore) limiterField.get(dispatcher);
    }
}
