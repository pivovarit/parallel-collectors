package com.pivovarit.collectors.parallelToCollection;

import com.pivovarit.collectors.infrastructure.CollectorUtils;
import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import com.pivovarit.collectors.infrastructure.TriFunction;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Grzegorz Piwowarek
 */
class ToCollectionRejectedExecutionHandlingTest extends ExecutorAwareTest {
    @TestFactory
    Collection<DynamicTest> dynamicTestsCollector() {
        ArrayList<DynamicTest> tests = new ArrayList<>();

        CollectorUtils
                .<Supplier<Integer>>getCollectorsAsLamdas()
                .forEach(collector -> {
                    tests.add(DynamicTest.dynamicTest("shouldCollectToCollectionAndSurviveRejectedExecutionException", () -> shouldCollectToCollectionAndSurviveRejectedExecutionException(collector)));
                    tests.add(DynamicTest.dynamicTest("shouldCollectToCollectionAndSurviveRejectedExecutionExceptionUnbounded", () -> shouldCollectToCollectionAndSurviveRejectedExecutionExceptionUnbounded(collector)));
                });

        return tests;
    }

    @TestFactory
    Collection<DynamicTest> dynamicTestsCollectorWithMapper() {
        ArrayList<DynamicTest> tests = new ArrayList<>();

        CollectorUtils
                .<Integer, Integer>getCollectorsAsLamdasWithMapper()
                .forEach(collector -> {
                    tests.add(DynamicTest.dynamicTest("shouldCollectToCollectionMappingAndSurviveRejectedExecutionException", () -> shouldCollectToCollectionMappingAndSurviveRejectedExecutionException(collector)));
                    tests.add(DynamicTest.dynamicTest("shouldCollectToCollectionMappingAndSurviveRejectedExecutionExceptionUnbounded", () -> shouldCollectToCollectionMappingAndSurviveRejectedExecutionExceptionUnbounded(collector)));
                });

        return tests;
    }

    void shouldCollectToCollectionAndSurviveRejectedExecutionException(BiFunction<Executor, Integer, Collector<Supplier<Supplier<Integer>>, List<CompletableFuture<Supplier<Integer>>>, ? extends CompletableFuture<? extends Collection<Supplier<Integer>>>>> collector) {
        // given
        executor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 1000).boxed()
          .map(i -> supplier(() -> supplier(() -> returnWithDelay(i, ofMillis(10000)))))
          .collect(collector.apply(executor, 10000))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }

    void shouldCollectToCollectionAndSurviveRejectedExecutionExceptionUnbounded(BiFunction<Executor, Integer, Collector<Supplier<Supplier<Integer>>, List<CompletableFuture<Supplier<Integer>>>, ? extends CompletableFuture<? extends Collection<Supplier<Integer>>>>> collector) {
        // given
        executor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 1000).boxed()
          .map(i -> supplier(() -> supplier(() -> returnWithDelay(i, ofMillis(10000)))))
          .collect(collector.apply(executor, Integer.MAX_VALUE))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }

    void shouldCollectToCollectionMappingAndSurviveRejectedExecutionException(TriFunction<Function<Integer, Integer>, Executor, Integer, Collector<Integer, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector) {
        // given
        executor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 1000).boxed()
          .collect(collector.apply(i -> returnWithDelay(i, ofMillis(10000)), executor, 10))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }

    void shouldCollectToCollectionMappingAndSurviveRejectedExecutionExceptionUnbounded(TriFunction<Function<Integer, Integer>, Executor, Integer, Collector<Integer, List<CompletableFuture<Integer>>, ? extends CompletableFuture<? extends Collection<Integer>>>> collector) {
        // given
        executor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 1000).boxed()
          .collect(collector.apply(i -> returnWithDelay(i, ofMillis(10000)), executor, Integer.MAX_VALUE))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }
}
