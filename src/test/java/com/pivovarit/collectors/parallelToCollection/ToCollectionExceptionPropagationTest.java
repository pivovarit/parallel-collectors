package com.pivovarit.collectors.parallelToCollection;

import com.pivovarit.collectors.infrastructure.CollectorUtils;
import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import com.pivovarit.collectors.infrastructure.TestUtils;
import com.pivovarit.collectors.infrastructure.TriFunction;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TestUtils.throwing;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Grzegorz Piwowarek
 */
class ToCollectionExceptionPropagationTest extends ExecutorAwareTest {
    @TestFactory
    Collection<DynamicTest> dynamicTestsCollector() {
        ArrayList<DynamicTest> tests = new ArrayList<>();

        CollectorUtils
                .<Integer>getCollectorsAsLamdas()
                .forEach(collector -> {
                    tests.add(DynamicTest.dynamicTest("shouldCollectToCollectionAndNotSwallowException", () -> shouldCollectToCollectionAndNotSwallowException(collector)));
                    tests.add(DynamicTest.dynamicTest("shouldCollectToCollectionAndNotSwallowExceptionUnbounded", () -> shouldCollectToCollectionAndNotSwallowExceptionUnbounded(collector)));
                });

        return tests;
    }

    @TestFactory
    Collection<DynamicTest> dynamicTestsCollectorWithMapper() {
        ArrayList<DynamicTest> tests = new ArrayList<>();

        CollectorUtils
                .<Integer, Integer>getCollectorsAsLamdasWithMapper()
                .forEach(collector -> {
                    tests.add(DynamicTest.dynamicTest("shouldCollectToCollectionMappingAndNotSwallowException", () -> shouldCollectToCollectionMappingAndNotSwallowException(collector)));
                    tests.add(DynamicTest.dynamicTest("shouldCollectToCollectionMappingAndNotSwallowExceptionUnbounded", () -> shouldCollectToCollectionMappingAndNotSwallowExceptionUnbounded(collector)));
                });

        return tests;
    }

    void shouldCollectToCollectionAndNotSwallowException(BiFunction<Executor, Integer, Collector<Supplier<Integer>, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector) {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(IntStream.range(0, 10).boxed()
                .map(i -> supplier(() -> throwing(i)))
                .collect(collector.apply(executor, 10))::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }


    void shouldCollectToCollectionAndNotSwallowExceptionUnbounded(BiFunction<Executor, Integer, Collector<Supplier<Integer>, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector) {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(IntStream.range(0, 10).boxed()
                .map(i -> supplier(() -> throwing(i)))
                .collect(collector.apply(executor, Integer.MAX_VALUE))::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    void shouldCollectToCollectionMappingAndNotSwallowException(TriFunction<Function<Integer, Integer>, Executor, Integer, Collector<Integer, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector) {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(IntStream.range(0, 10).boxed()
                .collect(collector.apply(TestUtils::throwing, executor, 10))::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    void shouldCollectToCollectionMappingAndNotSwallowExceptionUnbounded(TriFunction<Function<Integer, Integer>, Executor, Integer, Collector<Integer, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector) {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(IntStream.range(0, 10).boxed()
                .collect(collector.apply(TestUtils::throwing, executor, Integer.MAX_VALUE))::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }
}
