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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

class DispatcherTest {

    @Test
    void shouldRejectNullExecutor() {
        assertThatThrownBy(() -> new Dispatcher<>(null, 1))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("Executor can't be null");

        assertThatThrownBy(() -> new Dispatcher<>(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("Executor can't be null");
    }

    @Test
    void shouldRejectExecutorWithDiscardPolicy() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
          1, 1, 0L, TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<>(),
          new ThreadPoolExecutor.DiscardPolicy());

        assertThatThrownBy(() -> new Dispatcher<>(executor, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("discard tasks");
    }

    @Test
    void shouldSubmitAndCompleteTask() {
        Executor executor = Runnable::run;
        Dispatcher<Integer> dispatcher = new Dispatcher<>(executor);

        dispatcher.start();
        CompletableFuture<Integer> result = dispatcher.submit(() -> 42);
        dispatcher.stop();

        assertThat(result.join()).isEqualTo(42);
    }

    @Test
    void shouldNotBeRunningBeforeStart() {
        Executor executor = Runnable::run;
        Dispatcher<Integer> dispatcher = new Dispatcher<>(executor);

        assertThat(dispatcher.isRunning()).isFalse();
    }

    @Test
    void shouldBeRunningAfterStart() {
        Executor executor = Runnable::run;
        Dispatcher<Integer> dispatcher = new Dispatcher<>(executor);

        dispatcher.start();

        await().atMost(1, SECONDS).until(dispatcher::isRunning);
        assertThat(dispatcher.isRunning()).isTrue();

        dispatcher.stop();
    }

    @Test
    void shouldHandleMultipleTasksSequentially() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Dispatcher<Integer> dispatcher = new Dispatcher<>(executor);

        dispatcher.start();

        List<CompletableFuture<Integer>> futures = List.of(
          dispatcher.submit(() -> 1),
          dispatcher.submit(() -> 2),
          dispatcher.submit(() -> 3)
        );

        dispatcher.stop();

        assertThat(futures.get(0).join()).isEqualTo(1);
        assertThat(futures.get(1).join()).isEqualTo(2);
        assertThat(futures.get(2).join()).isEqualTo(3);

        executor.shutdown();
    }

    @Test
    void shouldRespectParallelismLimit() throws Exception {
        CountDownLatch taskStarted = new CountDownLatch(2);
        CountDownLatch proceedSignal = new CountDownLatch(1);
        AtomicInteger concurrentTasks = new AtomicInteger(0);
        AtomicInteger maxConcurrentTasks = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        Dispatcher<Integer> dispatcher = new Dispatcher<>(executor, 2);

        dispatcher.start();

        List<CompletableFuture<Integer>> futures = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int value = i;
            futures.add(dispatcher.submit(() -> {
                int current = concurrentTasks.incrementAndGet();
                maxConcurrentTasks.updateAndGet(max -> Math.max(max, current));
                taskStarted.countDown();
                try {
                    proceedSignal.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                concurrentTasks.decrementAndGet();
                return value;
            }));
        }

        // Wait for at least 2 tasks to start
        assertThat(taskStarted.await(5, SECONDS)).isTrue();

        // Give a bit of time to ensure no more than 2 tasks start
        Thread.sleep(100);

        // Should have at most 2 concurrent tasks due to parallelism limit
        assertThat(maxConcurrentTasks.get()).isLessThanOrEqualTo(2);

        // Release all tasks
        proceedSignal.countDown();

        dispatcher.stop();

        // All tasks should complete
        futures.forEach(CompletableFuture::join);

        executor.shutdown();
    }

    @Test
    void shouldPropagateExceptionFromTask() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Dispatcher<Integer> dispatcher = new Dispatcher<>(executor);

        dispatcher.start();

        CompletableFuture<Integer> result = dispatcher.submit(() -> {
            throw new RuntimeException("Task failed");
        });

        dispatcher.stop();

        assertThatThrownBy(result::join)
          .hasCauseInstanceOf(RuntimeException.class)
          .hasMessageContaining("Task failed");

        executor.shutdown();
    }

    @Test
    void shouldAllowStartOnlyOnce() {
        Executor executor = Runnable::run;
        Dispatcher<Integer> dispatcher = new Dispatcher<>(executor);

        dispatcher.start();
        dispatcher.start(); // Second start should be ignored

        CompletableFuture<Integer> result = dispatcher.submit(() -> 42);
        dispatcher.stop();

        assertThat(result.join()).isEqualTo(42);
    }

    @Test
    void shouldHandleTaskSubmissionAfterException() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Dispatcher<Integer> dispatcher = new Dispatcher<>(executor);

        dispatcher.start();

        // Submit a failing task
        CompletableFuture<Integer> failedResult = dispatcher.submit(() -> {
            throw new RuntimeException("First task failed");
        });

        // The failing task should propagate exception to subsequently submitted tasks
        CompletableFuture<Integer> secondResult = dispatcher.submit(() -> 99);

        dispatcher.stop();

        assertThatThrownBy(failedResult::join)
          .hasCauseInstanceOf(RuntimeException.class);

        // Second task should also fail due to dispatcher being in failed state
        assertThat(secondResult.isCompletedExceptionally()).isTrue();

        executor.shutdown();
    }

    @Test
    void shouldCancelFutureWhenDispatcherFails() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Dispatcher<Integer> dispatcher = new Dispatcher<>(executor);

        dispatcher.start();

        // Submit a task that will complete normally (let it complete first)
        CompletableFuture<Integer> normalTask = dispatcher.submit(() -> 42);
        normalTask.join(); // Wait for it to complete

        // Submit a failing task
        CompletableFuture<Integer> failingTask = dispatcher.submit(() -> {
            throw new RuntimeException("Dispatcher failure");
        });

        // Submit another task after the failing one
        CompletableFuture<Integer> subsequentTask = dispatcher.submit(() -> 100);

        dispatcher.stop();

        // Subsequent task should be cancelled/failed after the dispatcher fails
        // Give some time for the failure to propagate
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(subsequentTask.isCompletedExceptionally() || subsequentTask.isCancelled()).isTrue();

        executor.shutdown();
    }

    @Test
    void shouldWorkWithUnlimitedParallelism() {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Dispatcher<Integer> dispatcher = new Dispatcher<>(executor);

        dispatcher.start();

        List<CompletableFuture<Integer>> futures = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 100; i++) {
            final int value = i;
            futures.add(dispatcher.submit(() -> value));
        }

        dispatcher.stop();

        assertThat(futures).allMatch(f -> !f.isCompletedExceptionally());
        assertThat(futures.stream().map(CompletableFuture::join).toList())
          .containsExactly(java.util.stream.IntStream.range(0, 100).boxed().toArray(Integer[]::new));

        executor.shutdown();
    }
}
