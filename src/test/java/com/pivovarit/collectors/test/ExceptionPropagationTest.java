package com.pivovarit.collectors.test;

import com.pivovarit.collectors.ParallelCollectors;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.pivovarit.collectors.test.ExceptionPropagationTest.CollectorDefinition.collector;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExceptionPropagationTest {

    private static Stream<CollectorDefinition<Integer, Integer>> all() {
        return Stream.of(
          collector("parallel()", f -> collectingAndThen(ParallelCollectors.parallel(f), c -> c.join().toList())),
          collector("parallel(e)", f -> collectingAndThen(ParallelCollectors.parallel(f, e()), c -> c.join().toList())),
          collector("parallel(e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, e(), p()), c -> c.join().toList())),
          collector("parallel(toList())", f -> collectingAndThen(ParallelCollectors.parallel(f, toList()), CompletableFuture::join)),
          collector("parallel(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e()), CompletableFuture::join)),
          collector("parallel(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallelToStream()", f -> collectingAndThen(ParallelCollectors.parallelToStream(f), Stream::toList)),
          collector("parallelToStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, e()), Stream::toList)),
          collector("parallelToStream(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, e(), p()), Stream::toList)),
          collector("parallelToStream(e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStream()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f), Stream::toList)),
          collector("parallelToOrderedStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e()), Stream::toList)),
          collector("parallelToOrderedStream(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStream(e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p()), Stream::toList))
        );
    }

    @TestFactory
    Stream<DynamicTest> shouldPropagateExceptionFactory() {
        return all()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              assertThatThrownBy(() -> IntStream.range(0, 10)
                .boxed()
                .collect(c.collector().apply(i -> {
                    if (i == 7) {
                        throw new IllegalArgumentException();
                    } else {
                        return i;
                    }
                })))
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
          }));
    }

    record CollectorDefinition<T, R>(String name, Function<Function<T, R>, Collector<T, ?, List<R>>> collector) {
        static <T, R> CollectorDefinition<T, R> collector(String name, Function<Function<T, R>, Collector<T, ?, List<R>>> collector) {
            return new CollectorDefinition<>(name, collector);
        }
    }

    private static Executor e() {
        return Executors.newCachedThreadPool();
    }

    private static int p() {
        return 4;
    }
}
