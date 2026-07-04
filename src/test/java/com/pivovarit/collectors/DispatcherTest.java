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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void shouldNotExecuteTasksSubmittedAfterFailure() throws Exception {
        var executor = Executors.newCachedThreadPool();
        var dispatcherThread = new AtomicReference<Thread>();
        var dispatcher = new Dispatcher<Integer>(executor, dispatcherThread::set);
        dispatcher.start();

        var failed = dispatcher.submit(() -> {
            throw new IllegalStateException("boom");
        });
        assertThatThrownBy(failed::join).hasCauseInstanceOf(IllegalStateException.class);

        var executed = new AtomicBoolean();
        var straggler = dispatcher.submit(() -> {
            executed.set(true);
            return 42;
        });

        dispatcher.stop();
        dispatcherThread.get().join();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(straggler).isCompletedExceptionally();
        assertThat(executed)
          .as("task submitted after failure should not execute")
          .isFalse();
    }

    @Test
    void shouldStopWhenCallingThreadIsInterrupted() throws Exception {
        var holder = new AtomicReference<Thread>();
        var dispatcher = new Dispatcher<Integer>(Executors.newCachedThreadPool(), 1, holder::set);
        dispatcher.start();
        var future = dispatcher.submit(() -> 42);

        var interruptPreserved = new AtomicBoolean(false);
        var stopper = Thread.ofVirtual().start(() -> {
            Thread.currentThread().interrupt();
            dispatcher.stop();
            interruptPreserved.set(Thread.currentThread().isInterrupted());
        });
        stopper.join();

        assertThat(interruptPreserved)
          .as("interrupt status should be preserved")
          .isTrue();
        assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo(42);
        await().untilAsserted(() -> assertThat(holder.get().isAlive()).isFalse());
    }

    @Test
    void shouldStopWhenTerminationGuardCleanedOnInterruptedThread() throws Exception {
        var holder = new AtomicReference<Thread>();
        var dispatcher = new Dispatcher<Integer>(Executors.newCachedThreadPool(), 1, holder::set);
        dispatcher.start();
        var cleanable = dispatcher.registerTerminationGuard(new Object());

        var interruptPreserved = new AtomicBoolean(false);
        var cleaner = Thread.ofVirtual().start(() -> {
            Thread.currentThread().interrupt();
            cleanable.clean();
            interruptPreserved.set(Thread.currentThread().isInterrupted());
        });
        cleaner.join();

        assertThat(interruptPreserved)
          .as("interrupt status should be preserved")
          .isTrue();
        await().untilAsserted(() -> assertThat(holder.get().isAlive()).isFalse());
    }

    @Test
    void shouldNotLeakInterruptionIntoExecutorThreadOnTaskFailure() {
        var tasks = new ConcurrentLinkedQueue<Runnable>();
        var running = new AtomicBoolean(true);
        Thread.ofPlatform().start(() -> {
            while (running.get()) {
                var task = tasks.poll();
                if (task != null) {
                    task.run();
                } else {
                    Thread.onSpinWait();
                }
            }
        });

        try {
            var dispatcher = new Dispatcher<Integer>(tasks::add, 1);
            dispatcher.start();

            var failed = dispatcher.submit(() -> {
                throw new IllegalStateException("boom");
            });
            assertThatThrownBy(failed::join).hasCauseInstanceOf(IllegalStateException.class);
            dispatcher.stop();

            var interrupted = new AtomicReference<Boolean>();
            tasks.add(() -> interrupted.set(Thread.currentThread().isInterrupted()));

            await().untilAsserted(() -> assertThat(interrupted.get())
              .as("executor thread should not observe a leaked interrupt flag")
              .isFalse());
        } finally {
            running.set(false);
        }
    }

    @Test
    void shouldNotLeakInterruptionIntoExecutorThreadOnSiblingCancellation() throws Exception {
        var tasks = new ConcurrentLinkedQueue<Runnable>();
        var running = new AtomicBoolean(true);
        var leaked = new AtomicBoolean(false);
        var completedRuns = new AtomicInteger();

        for (int i = 0; i < 2; i++) {
            Thread.ofPlatform().start(() -> {
                while (running.get()) {
                    var task = tasks.poll();
                    if (task != null) {
                        task.run();
                        if (Thread.interrupted()) {
                            leaked.set(true);
                        }
                        completedRuns.incrementAndGet();
                    } else {
                        Thread.onSpinWait();
                    }
                }
            });
        }

        try {
            var dispatcher = new Dispatcher<Integer>(tasks::add, 2);
            dispatcher.start();

            var siblingStarted = new CountDownLatch(1);
            dispatcher.submit(() -> {
                siblingStarted.countDown();
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.onSpinWait();
                }
                return 42;
            });
            siblingStarted.await();

            var failed = dispatcher.submit(() -> {
                throw new IllegalStateException("boom");
            });
            assertThatThrownBy(failed::join).hasCauseInstanceOf(IllegalStateException.class);
            dispatcher.stop();

            await().untilAsserted(() -> assertThat(completedRuns.get()).isEqualTo(2));
            assertThat(leaked)
              .as("executor thread should not observe a leaked interrupt flag after sibling cancellation")
              .isFalse();
        } finally {
            running.set(false);
        }
    }

    @Test
    void shouldRejectSubmitAfterShutdown() {
        var dispatcher = new Dispatcher<Integer>(Executors.newCachedThreadPool(), 1);
        dispatcher.start();
        dispatcher.submit(() -> 42);
        dispatcher.stop();

        assertThatThrownBy(() -> dispatcher.submit(() -> 43))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("reused");
    }

    private static Semaphore extractSemaphore(Dispatcher<?> dispatcher) throws Exception {
        Field limiterField = Dispatcher.class.getDeclaredField("limiter");
        limiterField.setAccessible(true);
        return (Semaphore) limiterField.get(dispatcher);
    }
}
