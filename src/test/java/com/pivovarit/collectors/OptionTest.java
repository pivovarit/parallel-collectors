package com.pivovarit.collectors;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class OptionTest {

    @Test
    void shouldThrowOnInvalidParallelism() {
        assertThatThrownBy(() -> Option.parallelism(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowOnNullExecutor() {
        assertThatThrownBy(() -> Option.executor(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectExecutorWithDiscardPolicy() {
        try (var executor = new ThreadPoolExecutor(2, 4, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), new ThreadPoolExecutor.DiscardPolicy())) {
            assertThatThrownBy(() -> Option.executor(executor)).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Executor's RejectedExecutionHandler can't discard tasks");
        }
    }

    @Test
    void shouldRejectExecutorWithDiscardOldestPolicy() {
        try (var executor = new ThreadPoolExecutor(2, 4, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), new ThreadPoolExecutor.DiscardOldestPolicy())) {
            assertThatThrownBy(() -> Option.executor(executor)).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Executor's RejectedExecutionHandler can't discard tasks");
        }
    }

    @TestFactory
    Stream<DynamicTest> shouldThrowWhenSameOptionsAreUsedMultipleTimes() {
        return Stream.of(Option.batched(), Option.executor(r -> {}), Option.parallelism(1))
            .map(o -> DynamicTest.dynamicTest("should handle duplicated: " + nameOf(o), () -> {
                assertThatThrownBy(() -> Option.process(o, o))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessageContaining("each option can be used at most once, and you configured '%s' multiple times".formatted(nameOf(o)));
            }));
    }

    private String nameOf(Option option) {
        return switch (option) {
            case Option.Batching __ -> "batching";
            case Option.Parallelism __ -> "parallelism";
            case Option.ThreadPool __ -> "executor";
        };
    }

    @Nested
    class ConfigurationTests {

        record TestData(Optional<Boolean> batching, OptionalInt parallelism, Optional<Executor> executor) {
        }

        @TestFactory
        Stream<DynamicTest> shouldNotAllowCreatingConfigurationWithNullOptionals() {
            return testData()
              .map(data ->
                DynamicTest.dynamicTest("Batching[%s], Parallelism[%s], Executor[%s]".formatted(printable(data.batching), printable(data.parallelism), printable(data.executor)),
                () -> Assertions.assertThatThrownBy(() -> new Option.Configuration(data.batching, data.parallelism, data.executor)).isInstanceOf(NullPointerException.class)
              ));
        }

        private static Stream<TestData> testData() {
            var batchings = Stream.of(null, Optional.<Boolean>empty(), Optional.of(true)).toList();
            var parallelisms = Stream.of(null, OptionalInt.empty(), OptionalInt.of(42)).toList();
            var executors = Stream.of(null, Optional.<Executor>empty(), Optional.<Executor>of(Executors.newVirtualThreadPerTaskExecutor())).toList();

            return batchings.stream()
              .flatMap(b -> parallelisms.stream()
                .flatMap(p -> executors.stream()
                  .map(e -> new TestData(b, p, e))))
              .filter(data -> data.batching == null || data.parallelism == null || data.executor == null);
        }

        private String printable(Object o) {
            return switch (o) {
                case null -> "null";
                case Optional<?> opt -> opt.map(obj -> "some(%s)".formatted(obj instanceof Executor ? "executor" : obj.toString())).orElse("empty");
                case OptionalInt opt -> opt.isPresent() ? "some(%d)".formatted(opt.getAsInt()) : "empty";
                default -> o.toString();
            };
        }
    }
}
