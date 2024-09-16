package com.pivovarit.collectors.test;

import com.pivovarit.collectors.ParallelCollectors;
import com.pivovarit.collectors.TestUtils;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.pivovarit.collectors.test.BasicParallelismTest.CollectorDefinition.collector;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class BasicParallelismTest {

    private static Stream<CollectorDefinition<Integer, Integer>> allBounded() {
        return Stream.of(
          collector("parallel(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, e(), p), c -> c.join()
            .toList())),
          collector("parallel(toList(), e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e(), p), CompletableFuture::join)),
          collector("parallel(toList(), e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e(), p), CompletableFuture::join)),
          collector("parallelToStream(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, e(), p), Stream::toList)),
          collector("parallelToStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e(), p), Stream::toList)),
          collector("parallelToOrderedStream(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e(), p), Stream::toList)),
          collector("parallelToOrderedStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p), Stream::toList))
        );
    }

    @TestFactory
    Stream<DynamicTest> shouldProcessEmptyWithMaxParallelism() {
        return Stream.of(1, 2, 4, 8, 16, 32, 64, 100)
          .flatMap(p -> allBounded()
            .map(c -> DynamicTest.dynamicTest("%s (parallelism: %d)".formatted(c.name(), p), () -> {
                assertThat(Stream.<Integer>empty().collect(c.factory().collector(i -> i, p))).isEmpty();
            })));
    }

    @TestFactory
    Stream<DynamicTest> shouldProcessAllElementsWithMaxParallelism() {
        return Stream.of(1, 2, 4, 8, 16, 32, 64, 100)
          .flatMap(p -> allBounded()
            .map(c -> DynamicTest.dynamicTest("%s (parallelism: %d)".formatted(c.name(), p), () -> {
                var list = IntStream.range(0, 100).boxed().toList();
                List<Integer> result = list.stream().collect(c.factory().collector(i -> i, p));
                assertThat(result).containsExactlyInAnyOrderElementsOf(list);
            })));
    }

    @TestFactory
    Stream<DynamicTest> shouldRespectMaxParallelism() {
        return allBounded()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var duration = timed(() -> IntStream.range(0, 10).boxed()
                .collect(c.factory().collector(i -> TestUtils.returnWithDelay(i, Duration.ofMillis(100)), 2)));
              assertThat(duration).isCloseTo(Duration.ofMillis(500), Duration.ofMillis(100));
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldRejectInvalidParallelism() {
        return allBounded()
          .flatMap(c -> Stream.of(-1, 0)
            .map(p -> DynamicTest.dynamicTest("%s [p=%d]".formatted(c.name(), p), () -> {
                assertThatThrownBy(() -> Stream.of(1).collect(c.factory().collector(i -> i, p)))
                  .isExactlyInstanceOf(IllegalArgumentException.class);
            })));
    }

    protected record CollectorDefinition<T, R>(String name, CollectorFactory<T, R> factory) {
        static <T, R> CollectorDefinition<T, R> collector(String name, CollectorFactory<T, R> collector) {
            return new CollectorDefinition<>(name, collector);
        }
    }

    @FunctionalInterface
    private interface CollectorFactory<T, R> {
        Collector<T, ?, List<R>> collector(Function<T, R> f, Integer p);
    }

    private static Executor e() {
        return Executors.newCachedThreadPool();
    }

    private static Duration timed(Supplier<?> action) {
        long start = System.currentTimeMillis();
        var result = action.get();
        return Duration.ofMillis(System.currentTimeMillis() - start);
    }
}
