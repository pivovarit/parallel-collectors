package com.pivovarit.collectors;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class CompletionOrderSpliteratorTest {

    @Test
    void shouldTraverseInCompletionOrder() {
        CompletableFuture<Integer> f1 = new CompletableFuture<>();
        CompletableFuture<Integer> f2 = new CompletableFuture<>();
        CompletableFuture<Integer> f3 = new CompletableFuture<>();
        List<CompletableFuture<Integer>> futures = asList(f1, f2, f3);

        CompletableFuture.runAsync(() -> {
            sleep(10);
            f3.complete(3);
            sleep(10);
            f1.complete(2);
            sleep(10);
            f2.complete(1);
        });
        List<Integer> results = StreamSupport.stream(
          new CompletionOrderSpliterator<>(futures), false)
          .collect(Collectors.toList());

        assertThat(results).containsExactly(3, 2, 1);
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

        CompletionOrderSpliterator<Integer> spliterator = new CompletionOrderSpliterator<>(futures);

        ResultHolder<Integer> result = new ResultHolder<>();
        boolean consumed = spliterator.tryAdvance(result);

        assertThat(consumed).isFalse();
        assertThat(result.result).isNull();
    }

    static class ResultHolder<T> implements Consumer<T> {

        private T result;

        @Override
        public void accept(T t) {
            result = t;
        }
    }
}
