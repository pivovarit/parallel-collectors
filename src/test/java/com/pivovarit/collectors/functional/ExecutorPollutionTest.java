package com.pivovarit.collectors.functional;

import com.pivovarit.collectors.ParallelCollectors;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

class ExecutorPollutionTest {

    @TestFactory
    Stream<DynamicTest> shouldStartProcessingElementsTests() {
        return of(
          shouldNotSubmitMoreTasksThanParallelism(ParallelCollectors::parallel, "parallel#1"),
          shouldNotSubmitMoreTasksThanParallelism((f, e, p) -> ParallelCollectors.parallel(f, toList(), e, p), "parallel#2"),
          shouldNotSubmitMoreTasksThanParallelism(ParallelCollectors::parallelToStream, "parallelToStream"),
          shouldNotSubmitMoreTasksThanParallelism(ParallelCollectors::parallelToOrderedStream, "parallelToOrderedStream"),
          shouldNotSubmitMoreTasksThanParallelism(ParallelCollectors.Batching::parallel, "parallel#1 (batching)"),
          shouldNotSubmitMoreTasksThanParallelism((f, e, p) -> ParallelCollectors.Batching.parallel(f, toList(), e, p), "parallel#2 (batching)"),
          shouldNotSubmitMoreTasksThanParallelism(ParallelCollectors.Batching::parallelToStream, "parallelToStream (batching)"),
          shouldNotSubmitMoreTasksThanParallelism(ParallelCollectors.Batching::parallelToOrderedStream, "parallelToOrderedStream (batching)")
        );
    }

    private static DynamicTest shouldNotSubmitMoreTasksThanParallelism(CollectorFactory<Integer> collector, String name) {
        return DynamicTest.dynamicTest(name, () -> {
            try (var e = warmedUp(new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(2)))) {

                var result = Stream.generate(() -> 42)
                  .limit(1000)
                  .collect(collector.apply(i -> i, e, 2));

                switch (result) {
                    case CompletableFuture<?> cf -> cf.join();
                    case Stream<?> s -> s.forEach((__) -> {});
                    default -> throw new IllegalStateException("can't happen");
                }
            }
        });
    }

    interface CollectorFactory<T> {
        Collector<T, ?, ?> apply(Function<T, ?> function, Executor executorService, int parallelism);
    }

    private static ThreadPoolExecutor warmedUp(ThreadPoolExecutor e) {
        for (int i = 0; i < e.getCorePoolSize(); i++) {
            e.submit(() -> {});
        }
        return e;
    }
}
