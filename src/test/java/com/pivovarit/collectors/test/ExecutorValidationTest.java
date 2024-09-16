package com.pivovarit.collectors.test;

import com.pivovarit.collectors.ParallelCollectors;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.pivovarit.collectors.test.ExecutorValidationTest.CollectorDefinition.collector;
import static java.util.stream.Collectors.collectingAndThen;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutorValidationTest {

    private static Stream<CollectorDefinition<Integer, Integer>> allWithCustomExecutors() {
        return Stream.of(
          collector("parallel(e)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, e), c -> c.thenApply(Stream::toList).join())),
          collector("parallel(e, p=1)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, e, 1), c -> c.thenApply(Stream::toList).join())),
          collector("parallel(e, p=4)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, e, 4), c -> c.thenApply(Stream::toList).join())),
          collector("parallel(e, p=1) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallel(f, e, 1), c -> c.thenApply(Stream::toList).join())),
          collector("parallel(e, p=4) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallel(f, e, 4), c -> c.thenApply(Stream::toList).join())),
          collector("parallelToStream(e)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, e), Stream::toList)),
          collector("parallelToStream(e, p=1)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, e, 1), Stream::toList)),
          collector("parallelToStream(e, p=4)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, e, 4), Stream::toList)),
          collector("parallelToStream(e, p=1) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e, 1), Stream::toList)),
          collector("parallelToStream(e, p=4) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e, 4), Stream::toList)),
          collector("parallelToOrderedStream(e, p=1)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e, 1), Stream::toList)),
          collector("parallelToOrderedStream(e, p=4)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e, 4), Stream::toList)),
          collector("parallelToOrderedStream(e, p=1) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e, 1), Stream::toList)),
          collector("parallelToOrderedStream(e, p=4) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e, 4), Stream::toList))
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

    protected record CollectorDefinition<T, R>(String name, Factory.CollectorFactoryWithExecutor<T, R> factory) {
        static <T, R> CollectorDefinition<T, R> collector(String name, Factory.CollectorFactoryWithExecutor<T, R> factory) {
            return new CollectorDefinition<>(name, factory);
        }
    }


}
