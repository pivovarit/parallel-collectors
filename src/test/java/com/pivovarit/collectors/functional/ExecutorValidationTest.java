package com.pivovarit.collectors.functional;

import com.pivovarit.collectors.ParallelCollectors;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutorValidationTest {

    @TestFactory
    Stream<DynamicTest> shouldStartProcessingElementsTests() {
        return of(
          shouldRejectInvalidRejectedExecutionHandler(e -> ParallelCollectors.parallel(i -> i, e, 2), "parallel"),
          shouldRejectInvalidRejectedExecutionHandler(e -> ParallelCollectors.parallelToStream(i -> i, e, 2), "parallelToStream"),
          shouldRejectInvalidRejectedExecutionHandler(e -> ParallelCollectors.parallelToOrderedStream(i -> i, e, 2), "parallelToOrderedStream"),
          shouldRejectInvalidRejectedExecutionHandler(e -> ParallelCollectors.Batching.parallel(i -> i, e, 2), "parallel (batching)"),
          shouldRejectInvalidRejectedExecutionHandler(e -> ParallelCollectors.Batching.parallelToStream(i -> i, e, 2), "parallelToStream (batching)"),
          shouldRejectInvalidRejectedExecutionHandler(e -> ParallelCollectors.Batching.parallelToOrderedStream(i -> i, e, 2), "parallelToOrderedStream (batching)")
        ).flatMap(i -> i);
    }

    private static Stream<DynamicTest> shouldRejectInvalidRejectedExecutionHandler(Function<ExecutorService, Collector<Integer, ?, ?>> collector, String name) {
        return Stream.of(new ThreadPoolExecutor.DiscardOldestPolicy(), new ThreadPoolExecutor.DiscardPolicy())
          .map(dp -> DynamicTest.dynamicTest(name + " : " + dp.getClass().getSimpleName(), () -> {
              try (var e = new ThreadPoolExecutor(2, 2000, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), dp)) {
                  assertThatThrownBy(() -> Stream.of(1, 2, 3)
                    .collect(collector.apply(e))).isExactlyInstanceOf(IllegalArgumentException.class);
              }
          }));
    }
}
