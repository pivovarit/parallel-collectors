package com.pivovarit.collectors.test;

import com.pivovarit.collectors.ParallelCollectors;
import com.pivovarit.collectors.TestUtils;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.test.Factory.GenericCollector.executorCollector;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class RejectedExecutionHandlingTest {

    private static Stream<Factory.GenericCollector<Factory.CollectorFactoryWithExecutor<Integer, Integer>>> allWithCustomExecutors() {
        return Stream.of(
          executorCollector("parallel(e)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, e), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallel(e, p=4)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, e, 4), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallel(e, p=4) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallel(f, e, 4), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallelToStream(e)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, e), Stream::toList)),
          executorCollector("parallelToStream(e, p=4)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, e, 4), Stream::toList)),
          executorCollector("parallelToStream(e, p=4) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e, 4), Stream::toList)),
          executorCollector("parallelToOrderedStream(e, p=4)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e, 4), Stream::toList)),
          executorCollector("parallelToOrderedStream(e, p=4) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e, 4), Stream::toList))
        );
    }

    @TestFactory
    Stream<DynamicTest> shouldRejectInvalidRejectedExecutionHandlerFactory() {
        return allWithCustomExecutors()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              assertThatThrownBy(() -> {
                  try (var e = new ThreadPoolExecutor(2, 2, 0L, MILLISECONDS,
                    new LinkedBlockingQueue<>(1), new ThreadPoolExecutor.AbortPolicy())) {
                      assertTimeoutPreemptively(ofMillis(100), () -> of(1, 2, 3, 4)
                        .collect(c.factory().collector(i -> TestUtils.sleepAndReturn(1_000, i), e)));
                  }
              }).isExactlyInstanceOf(CompletionException.class);
          }));
    }

    private static Stream<Factory.GenericCollector<Factory.CollectorFactoryWithExecutor<Integer, Integer>>> allWithCustomExecutorsParallelismOne() {
        return Stream.of(
          executorCollector("parallel(e, p=1)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, e, 1), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallel(e, p=1) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.Batching.parallel(f, e, 1), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallelToStream(e, p=1)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, e, 1), Stream::toList)),
          executorCollector("parallelToOrderedStream(e, p=1)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e, 1), Stream::toList))
        );
    }

    @TestFactory
    Stream<DynamicTest> shouldRejectInvalidRejectedExecutionHandlerWhenParallelismOneFactory() {
        return allWithCustomExecutorsParallelismOne()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var e = new ThreadPoolExecutor(1, 1, 0L, SECONDS, new LinkedBlockingQueue<>(1));
              e.submit(() -> TestUtils.sleepAndReturn(10_000, 42));
              e.submit(() -> TestUtils.sleepAndReturn(10_000, 42));
              assertThatThrownBy(() -> {
                  assertTimeoutPreemptively(ofMillis(100), () -> of(1, 2, 3, 4)
                    .collect(c.factory().collector(i -> TestUtils.sleepAndReturn(1_000, i), e)));
              }).isExactlyInstanceOf(CompletionException.class);
          }));
    }
}
