package com.pivovarit.collectors.parallelToCollection;

import com.pivovarit.collectors.infrastructure.CollectorUtils;
import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import com.pivovarit.collectors.infrastructure.TestUtils;
import com.pivovarit.collectors.infrastructure.TriFunction;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TestUtils.throwing;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Grzegorz Piwowarek
 */
class ToCollectionExceptionPropagationTest extends ExecutorAwareTest {
    @TestFactory
    Collection<DynamicTest> dynamicTestsCollector2() {
        return CollectorUtils
                .<Integer>getCollectorsAsLamdas2()
                .stream()
                .map(collector -> DynamicTest.dynamicTest("shouldCollectToCollectionAndNotSwallowException", () -> shouldCollectToCollectionAndNotSwallowException(collector)))
                .collect(Collectors.toList());
    }

    @TestFactory
    Collection<DynamicTest> dynamicTestsCollector1() {
        return CollectorUtils
                .<Integer>getCollectorsAsLamdas1()
                .stream()
                .map(collector -> DynamicTest.dynamicTest("shouldCollectToCollectionAndNotSwallowExceptionUnbounded", () -> shouldCollectToCollectionAndNotSwallowExceptionUnbounded(collector)))
                .collect(Collectors.toList());
    }

    @TestFactory
    Collection<DynamicTest> dynamicTestsCollectorWithMapper3() {
        return CollectorUtils
                .<Integer, Integer>getCollectorsAsLamdasWithMapper3()
                .stream()
                .map(collector -> DynamicTest.dynamicTest("shouldCollectToCollectionMappingAndNotSwallowException", () -> shouldCollectToCollectionMappingAndNotSwallowException(collector)))
                .collect(Collectors.toList());
    }

    @TestFactory
    Collection<DynamicTest> dynamicTestsCollectorWithMapper2() {
        return CollectorUtils
                .<Integer, Integer>getCollectorsAsLamdasWithMapper2()
                .stream()
                .map(collector -> DynamicTest.dynamicTest("shouldCollectToCollectionMappingAndNotSwallowExceptionUnbounded", () -> shouldCollectToCollectionMappingAndNotSwallowExceptionUnbounded(collector)))
                .collect(Collectors.toList());
    }

    @Test
    void shouldCollectToCollectionAndNotSwallowException(BiFunction<ThreadPoolExecutor, Integer, Collector<Supplier<Integer>, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector) {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(IntStream.range(0, 10).boxed()
                .map(i -> supplier(() -> throwing(i)))
                .collect(collector.apply(executor, 10))::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCollectToCollectionAndNotSwallowExceptionUnbounded(Function<ThreadPoolExecutor, Collector<Supplier<Integer>, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector) {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(IntStream.range(0, 10).boxed()
                .map(i -> supplier(() -> throwing(i)))
                .collect(collector.apply(executor))::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }


    @Test
    void shouldCollectToCollectionMappingAndNotSwallowException(TriFunction<Function<Integer, Integer>, ThreadPoolExecutor, Integer, Collector<Integer, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector) {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(IntStream.range(0, 10).boxed()
                .collect(collector.apply(TestUtils::throwing, executor, 10))::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCollectToCollectionMappingAndNotSwallowExceptionUnbounded(BiFunction<Function<Integer, Integer>, ThreadPoolExecutor, Collector<Integer, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector) {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(IntStream.range(0, 10).boxed()
                .collect(collector.apply(TestUtils::throwing, executor))::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }
}
