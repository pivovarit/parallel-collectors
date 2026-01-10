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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

class CompletionOrderSpliteratorTest {

    @Test
    void shouldTraverseInCompletionOrder() {
        CompletableFuture<Integer> f1 = new CompletableFuture<>();
        CompletableFuture<Integer> f2 = new CompletableFuture<>();
        CompletableFuture<Integer> f3 = new CompletableFuture<>();
        List<CompletableFuture<Integer>> futures = asList(f1, f2, f3);

        CompletableFuture.runAsync(() -> {
            f3.complete(3);
            sleep(100);
            f1.complete(2);
            sleep(100);
            f2.complete(1);
        });
        var results = StreamSupport.stream(new CompletionOrderSpliterator<>(futures), false).toList();

        assertThat(results).containsExactly(3, 2, 1);
    }

    @Test
    void shouldPropagateException() {
        CompletableFuture<Integer> f1 = new CompletableFuture<>();
        CompletableFuture<Integer> f2 = new CompletableFuture<>();
        CompletableFuture<Integer> f3 = new CompletableFuture<>();
        List<CompletableFuture<Integer>> futures = asList(f1, f2, f3);

        CompletableFuture.runAsync(() -> {
            f3.complete(3);
            sleep(100);
            f1.completeExceptionally(new RuntimeException());
            sleep(100);
            f2.complete(1);
        });
        assertThatThrownBy(() -> StreamSupport.stream(new CompletionOrderSpliterator<>(futures), false).toList())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void shouldStreamInCompletionOrder() {
        int value = 42;
        List<CompletableFuture<Integer>> futures = asList(new CompletableFuture<>(), CompletableFuture
          .completedFuture(value));

        Optional<Integer> result = StreamSupport.stream(new CompletionOrderSpliterator<>(futures), false).findAny();

        assertThat(result).contains(value);
    }

    @Test
    void shouldNotConsumeOnEmpty() {
        List<CompletableFuture<Integer>> futures = Collections.emptyList();

        Spliterator<Integer> spliterator = new CompletionOrderSpliterator<>(futures);

        ResultHolder<Integer> result = new ResultHolder<>();
        boolean consumed = spliterator.tryAdvance(result);

        assertThat(consumed).isFalse();
        assertThat(result.result).isNull();
    }

    @Test
    void shouldRestoreInterrupt() {
        Thread executorThread = new Thread(() -> {
            Spliterator<Integer> spliterator = new CompletionOrderSpliterator<>(List.of(new CompletableFuture<>()));
            try {
                spliterator.tryAdvance(i -> {});
            } catch (Exception e) {
                while (true) {
                    Thread.onSpinWait();
                }
            }
        });

        executorThread.start();

        executorThread.interrupt();

        await()
          .until(executorThread::isInterrupted);
    }

    static class ResultHolder<T> implements Consumer<T> {

        private volatile T result;

        @Override
        public void accept(T t) {
            result = t;
        }
    }
}
