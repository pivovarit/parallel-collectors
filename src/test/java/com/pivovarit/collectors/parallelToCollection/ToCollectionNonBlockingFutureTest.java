package com.pivovarit.collectors.parallelToCollection;

import com.pivovarit.collectors.infrastructure.CollectorUtils;
import com.pivovarit.collectors.infrastructure.TriFunction;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * @author Grzegorz Piwowarek
 */
class ToCollectionNonBlockingFutureTest {
    private final Executor blockingExecutor = i -> {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };

    @TestFactory
    Collection<DynamicTest> dynamicTestsCollector() {
        ArrayList<DynamicTest> tests = new ArrayList<>();

        CollectorUtils
                .<Long>getCollectorsAsLamdas()
                .forEach(collector -> {
                    tests.add(DynamicTest.dynamicTest("shouldReturnImmediatelyCollection", () -> shouldReturnImmediatelyCollection(collector)));
                    tests.add(DynamicTest.dynamicTest("shouldReturnImmediatelyCollectionUnbounded", () -> shouldReturnImmediatelyCollectionUnbounded(collector)));
                });

        return tests;
    }

    @TestFactory
    Collection<DynamicTest> dynamicTestsCollectorWithMapper() {
        ArrayList<DynamicTest> tests = new ArrayList<>();

        CollectorUtils
                .<Supplier<Integer>, Long>getCollectorsAsLamdasWithMapper()
                .forEach(collector -> {
                    tests.add(DynamicTest.dynamicTest("shouldReturnImmediatelyCollectionMapping", () -> shouldReturnImmediatelyCollectionMapping(collector)));
                    tests.add(DynamicTest.dynamicTest("shouldReturnImmediatelyCollectionMappingUnbounded", () -> shouldReturnImmediatelyCollectionMappingUnbounded(collector)));
                });

        return tests;
    }

    void shouldReturnImmediatelyCollection(BiFunction<Executor, Integer, Collector<Supplier<Long>, List<CompletableFuture<Long>>, ? extends CompletableFuture<? extends Collection<Long>>>> collector) {
        assertTimeoutPreemptively(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(collector.apply(blockingExecutor, 42)));
    }

    void shouldReturnImmediatelyCollectionUnbounded(BiFunction<Executor, Integer, Collector<Supplier<Long>, List<CompletableFuture<Long>>, ? extends CompletableFuture<? extends Collection<Long>>>> collector) {
        assertTimeoutPreemptively(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(collector.apply(blockingExecutor, Integer.MAX_VALUE)));
    }

    void shouldReturnImmediatelyCollectionMapping(TriFunction<Function<Supplier<Integer>, Long>, Executor, Integer, Collector<Supplier<Integer>, List<CompletableFuture<Long>>, ? extends CompletableFuture<? extends Collection<Long>>>> collector) {
        assertTimeoutPreemptively(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(collector.apply(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), blockingExecutor, 42)));
    }

    void shouldReturnImmediatelyCollectionMappingUnbounded(TriFunction<Function<Supplier<Integer>, Long>, Executor, Integer, Collector<Supplier<Integer>, List<CompletableFuture<Long>>, ? extends CompletableFuture<? extends Collection<Long>>>> collector) {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(collector.apply(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), blockingExecutor, Integer.MAX_VALUE)));
    }


}
