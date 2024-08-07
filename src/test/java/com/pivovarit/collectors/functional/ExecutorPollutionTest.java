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

import static com.pivovarit.collectors.ParallelCollectors.Batching.parallel;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

class ExecutorPollutionTest {

    @TestFactory
    Stream<DynamicTest> shouldStartProcessingElementsTests() {
        return of(
          shouldNotSubmitMoreTasksThanParallelism((f, e, p) -> ParallelCollectors.parallel(f, e, p), "parallel#1"),
          shouldNotSubmitMoreTasksThanParallelism((f, __, p) -> ParallelCollectors.parallel(f, p), "parallel#2"),
          shouldNotSubmitMoreTasksThanParallelism((f, e, p) -> ParallelCollectors.parallel(f, toList(), e, p), "parallel#3"),
          shouldNotSubmitMoreTasksThanParallelism((f, __, p) -> ParallelCollectors.parallel(f, toList(), p), "parallel#4"),
          shouldNotSubmitMoreTasksThanParallelism((f, e, p) -> ParallelCollectors.parallelToStream(f, e, p), "parallelToStream#1"),
          shouldNotSubmitMoreTasksThanParallelism((f, __, p) -> ParallelCollectors.parallelToStream(f, p), "parallelToStream#2"),
          shouldNotSubmitMoreTasksThanParallelism((f, e, p) -> ParallelCollectors.parallelToOrderedStream(f, e, p), "parallelToOrderedStream#1"),
          shouldNotSubmitMoreTasksThanParallelism((f, __, p) -> ParallelCollectors.parallelToOrderedStream(f, p), "parallelToOrderedStream#2"),
          shouldNotSubmitMoreTasksThanParallelism((f, e, p) -> parallel(f, e, p), "parallel#1 (batching)"),
          shouldNotSubmitMoreTasksThanParallelism((f, e, p) -> parallel(f, toList(), e, p), "parallel#2 (batching)"),
          shouldNotSubmitMoreTasksThanParallelism((f, e, p) -> ParallelCollectors.Batching.parallelToStream(f, e, p), "parallelToStream (batching)"),
          shouldNotSubmitMoreTasksThanParallelism((f, e, p) -> ParallelCollectors.Batching.parallelToOrderedStream(f, e, p), "parallelToOrderedStream (batching)")
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
