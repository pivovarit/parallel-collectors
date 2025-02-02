package com.pivovarit.collectors.test;

import static com.pivovarit.collectors.test.Factory.GenericCollector.executorCollector;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pivovarit.collectors.ParallelCollectors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;


class ExecutorValidationTest {

    private static Stream<Factory.GenericCollector<Factory.CollectorFactoryWithExecutor<Integer, Integer>>> allWithCustomExecutors() {
        return Stream.of(
          executorCollector("parallel(e)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, e), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallel(e, p=1)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, e, 1), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallel(e, p=4)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, e, 4), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallel(e, p=1) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallel(f, e, 1), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallel(e, p=4) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallel(f, e, 4), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallel(toList(), e)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e), c -> c.join())),
          executorCollector("parallel(toList(), e, p=1)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e, 1), c -> c.join())),
          executorCollector("parallel(toList(), e, p=4)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e, 4), c -> c.join())),
          executorCollector("parallel(toList(), e, p=1) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e, 1), c -> c.join())),
          executorCollector("parallel(toList(), e, p=4) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e, 4), c -> c.join())),
          executorCollector("parallelToStream(e)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, e), Stream::toList)),
          executorCollector("parallelToStream(e, p=1)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, e, 1), Stream::toList)),
          executorCollector("parallelToStream(e, p=4)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, e, 4), Stream::toList)),
          executorCollector("parallelToStream(e, p=1) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e, 1), Stream::toList)),
          executorCollector("parallelToStream(e, p=4) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e, 4), Stream::toList)),
          executorCollector("parallelToOrderedStream(e, p=1)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e, 1), Stream::toList)),
          executorCollector("parallelToOrderedStream(e, p=4)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e, 4), Stream::toList)),
          executorCollector("parallelToOrderedStream(e, p=1) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e, 1), Stream::toList)),
          executorCollector("parallelToOrderedStream(e, p=4) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e, 4), Stream::toList))
        );
    }

    @TestFactory
    Stream<DynamicTest> shouldRejectInvalidRejectedExecutionHandlerFactory() {
        return allWithCustomExecutors()
          .flatMap(c -> Stream.of(new ThreadPoolExecutor.DiscardOldestPolicy(), new ThreadPoolExecutor.DiscardPolicy())
            .map(dp -> DynamicTest.dynamicTest("%s : %s".formatted(c.name(), dp.getClass().getSimpleName()), () -> {
                try (var e = new ThreadPoolExecutor(2, 2000, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), dp)) {
                    assertThatThrownBy(() -> Stream.of(1, 2, 3).collect(c.factory().collector(i -> i, e))).isExactlyInstanceOf(IllegalArgumentException.class);
                }
            })));
    }
}
