package com.pivovarit.collectors.parallelToCollection;

import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import com.pivovarit.collectors.infrastructure.TestUtils;
import com.pivovarit.collectors.infrastructure.TriFunction;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
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
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.*;
import static com.pivovarit.collectors.infrastructure.TestUtils.throwing;
import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Grzegorz Piwowarek
 */
class ToCollectionExceptionPropagationTest extends ExecutorAwareTest {
    @TestFactory
    Stream<DynamicTest> testCollectors() {
        return of(
                forCollector((mapper, e, para) -> parallelToSet(mapper, e, para), "parallelToSet(mapper)"),
                forCollector((mapper, e, para) -> parallelToList(mapper, e, para), "parallelToList(mapper)"),
                forCollector((mapper, e, para) -> parallelToCollection(mapper, ArrayList::new, e, para), "parallelToCollection(mapper)")
        ).flatMap(identity());
    }

    @Test
    Stream<DynamicTest> testCollectorsWithoutMapper() {
        return of(
                forCollector((e, para) -> parallelToSet(e, para), "parallelToSet()"),
                forCollector((e, para) -> parallelToList(e, para), "parallelToList()"),
                forCollector((e, para) -> parallelToCollection(ArrayList::new, e, para), "parallelToCollection()")
        ).flatMap(identity());
    }

    private static Stream<DynamicTest> forCollector(TriFunction<Function<Integer, Integer>, Executor, Integer, Collector<Integer, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector, String name) {
        return of(
                shouldCollectWithMappingAndNotSwallowException(collector, name),
                shouldCollectWithMappingAndNotSwallowExceptionUnbounded(collector, name));
    }

    private static Stream<DynamicTest> forCollector(BiFunction<Executor, Integer, Collector<Supplier<Integer>, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector, String name) {
        return of(
                shouldCollectAndNotSwallowException(collector, name),
                shouldCollectAndNotSwallowExceptionUnbounded(collector, name));
    }

    private static DynamicTest shouldCollectWithMappingAndNotSwallowException(TriFunction<Function<Integer, Integer>, Executor, Integer, Collector<Integer, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector, String name) {
        return DynamicTest.dynamicTest(format("%s: should collect with mapping and not swallow exception", name),
                () -> assertThatThrownBy(IntStream.range(0, 10).boxed()
                        .collect(collector.apply(TestUtils::throwing, threadPoolExecutor(10), 10))::join)
                        .isInstanceOf(CompletionException.class)
                        .hasCauseExactlyInstanceOf(IllegalArgumentException.class));
    }

    private static DynamicTest shouldCollectAndNotSwallowException(BiFunction<Executor, Integer, Collector<Supplier<Integer>, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector, String name) {
        return DynamicTest.dynamicTest(format("%s: should collect and not swallow exception", name),
                () -> assertThatThrownBy(IntStream.range(0, 10).boxed()
                        .map(i -> supplier(() -> throwing(i)))
                        .collect(collector.apply(threadPoolExecutor(10), 10))::join)
                        .isInstanceOf(CompletionException.class)
                        .hasCauseExactlyInstanceOf(IllegalArgumentException.class));
    }


    private static DynamicTest shouldCollectAndNotSwallowExceptionUnbounded(BiFunction<Executor, Integer, Collector<Supplier<Integer>, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector, String name) {
        return DynamicTest.dynamicTest(format("%s: should collect and not swallow exception unbounded", name),
                () -> assertThatThrownBy(IntStream.range(0, 10)
                        .boxed()
                        .map(i -> supplier(() -> throwing(i)))
                        .collect(collector.apply(threadPoolExecutor(10), Integer.MAX_VALUE))::join)
                        .isInstanceOf(CompletionException.class)
                        .hasCauseExactlyInstanceOf(IllegalArgumentException.class));
    }

    private static DynamicTest shouldCollectWithMappingAndNotSwallowExceptionUnbounded(TriFunction<Function<Integer, Integer>, Executor, Integer, Collector<Integer, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector, String name) {
        return DynamicTest.dynamicTest(format("%s: should collect with mapping and not swallow exception unbounded", name),
                () -> assertThatThrownBy(IntStream.range(0, 10).boxed()
                        .collect(collector.apply(TestUtils::throwing, threadPoolExecutor(10), Integer.MAX_VALUE))::join)
                        .isInstanceOf(CompletionException.class)
                        .hasCauseExactlyInstanceOf(IllegalArgumentException.class));
    }
}