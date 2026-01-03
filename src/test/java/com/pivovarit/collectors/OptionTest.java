package com.pivovarit.collectors;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class OptionTest {

    @Test
    void shouldThrowOnInvalidParallelism() {
        assertThatThrownBy(() -> Options.parallelism(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowOnNullExecutor() {
        assertThatThrownBy(() -> Options.executor(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectExecutorWithDiscardPolicy() {
        try (var executor = new ThreadPoolExecutor(2, 4, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), new ThreadPoolExecutor.DiscardPolicy())) {
            assertThatThrownBy(() -> Options.executor(executor)).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Executor's RejectedExecutionHandler can't discard tasks");
        }
    }

    @Test
    void shouldRejectExecutorWithDiscardOldestPolicy() {
        try (var executor = new ThreadPoolExecutor(2, 4, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), new ThreadPoolExecutor.DiscardOldestPolicy())) {
            assertThatThrownBy(() -> Options.executor(executor)).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Executor's RejectedExecutionHandler can't discard tasks");
        }
    }

    @TestFactory
    Stream<DynamicTest> shouldThrowWhenSameOptionsAreUsedMultipleTimes() {
        return Stream.<Options.CollectingOption>of(Options.batched(), Options.executor(r -> {}), Options.parallelism(1))
          .map(o -> DynamicTest.dynamicTest("should handle duplicated: " + nameOf(o), () -> {
              assertThatThrownBy(() -> ConfigProcessor.process(o, o))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("each option can be used at most once, and you configured '%s' multiple times".formatted(nameOf(o)));
          }));
    }

    private String nameOf(Options.CollectingOption option) {
        return switch (option) {
            case Options.Batched __ -> "batching";
            case Options.Parallelism __ -> "parallelism";
            case Options.ThreadPool __ -> "executor";
            case Options.Ordered __ -> "ordered";
        };
    }
}
