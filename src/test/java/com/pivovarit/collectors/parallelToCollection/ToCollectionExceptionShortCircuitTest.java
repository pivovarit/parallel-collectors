package com.pivovarit.collectors.parallelToCollection;

import com.pivovarit.collectors.infrastructure.CollectorUtils;
import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TestUtils.incrementAndThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToCollectionExceptionShortCircuitTest extends ExecutorAwareTest {

    private static final int T_POOL_SIZE = 4;

    @TestFactory
    Collection<DynamicTest> dynamicTestsCollector() {
        ArrayList<DynamicTest> tests = new ArrayList<>();

        CollectorUtils
                .getCollectorsAsLamdas()
                .forEach(collector -> {
                    tests.add(DynamicTest.dynamicTest("shouldCollectToCollectionAndShortCircuitOnException", () -> shouldCollectToCollectionAndShortCircuitOnException(collector)));
                    tests.add(DynamicTest.dynamicTest("shouldCollectToCollectionAndShortCircuitOnExceptionUnbounded", () -> shouldCollectToCollectionAndShortCircuitOnExceptionUnbounded(collector)));
                });

        return tests;
    }

    @BeforeEach
    void setup() {
        executor = threadPoolExecutor(T_POOL_SIZE);
    }

    void shouldCollectToCollectionAndShortCircuitOnException(BiFunction<Executor, Integer, Collector<Supplier<Object>, List<CompletableFuture<Object>>, ? extends CompletableFuture<? extends Collection<Object>>>> collector) {
        // given
        LongAdder counter = new LongAdder();

        assertThatThrownBy(IntStream.generate(() -> 42).boxed().limit(100)
          .map(i -> supplier(() -> incrementAndThrow(counter)))
          .collect(collector.apply(executor, 10))::join).isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);

        assertThat(counter.longValue()).isLessThanOrEqualTo(T_POOL_SIZE);
    }

    void shouldCollectToCollectionAndShortCircuitOnExceptionUnbounded(BiFunction<Executor, Integer, Collector<Supplier<Object>, List<CompletableFuture<Object>>, ? extends CompletableFuture<? extends Collection<Object>>>> collector) {
        // given
        LongAdder counter = new LongAdder();

        assertThatThrownBy(IntStream.generate(() -> 42).boxed().limit(100)
          .map(i -> supplier(() -> incrementAndThrow(counter)))
          .collect(collector.apply(executor, Integer.MAX_VALUE))::join).isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);

        assertThat(counter.longValue()).isLessThanOrEqualTo(T_POOL_SIZE);
    }
}
