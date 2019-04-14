package com.pivovarit.collectors.parallelToStream;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToStream;
import static com.pivovarit.collectors.infrastructure.TestUtils.TRIALS;
import static com.pivovarit.collectors.infrastructure.TestUtils.expectedDuration;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelayGaussian;
import static com.pivovarit.collectors.infrastructure.TestUtils.timed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * @author Grzegorz Piwowarek
 */
@RunWith(JUnitQuickcheck.class)
public class ToStreamParallelismThrottlingBDDTest extends ExecutorAwareTest {

    private static final long BLOCKING_MILLIS = 50;
    private static final long CONSTANT_DELAY = 100;

    @Property(trials = TRIALS)
    public void shouldCollectToListWithThrottledParallelism(@InRange(minInt = 2, maxInt = 20) int unitsOfWork, @InRange(minInt = 1, maxInt = 40) int parallelism) {
        // given
        executor = threadPoolExecutor(unitsOfWork);
        long expectedDuration = expectedDuration(parallelism, unitsOfWork, BLOCKING_MILLIS);

        Map.Entry<Stream<Long>, Long> result = timed(collectWith(f -> parallelToStream(f, executor, parallelism), unitsOfWork));

        assertThat(result)
          .satisfies(e -> {
              assertThat(e.getValue())
                .isGreaterThanOrEqualTo(expectedDuration)
                .isCloseTo(expectedDuration, offset(CONSTANT_DELAY));

              assertThat(e.getKey()).hasSize(unitsOfWork);
          });
    }

    @Property
    public void shouldMaintainOrder(@InRange(minInt = 2, maxInt = 20) int unitsOfWork, @InRange(minInt = 2, maxInt = 40) int parallelism) {
        // given
        executor = threadPoolExecutor(unitsOfWork);
        List<Integer> result = Stream.iterate(0, i -> i + 1).limit(20)
          .collect(parallelToStream(i -> returnWithDelayGaussian(i, Duration.ofMillis(10)), executor, parallelism))
          .join()
          .collect(Collectors.toList());

        assertThat(result).isSorted();
    }

    private static <R> Supplier<Stream<R>> collectWith(Function<UnaryOperator<Long>,  Collector<Long, ?, CompletableFuture<Stream<R>>>> collector, int unitsOfWork) {
        return () -> Stream.generate(() -> 42L)
            .limit(unitsOfWork)
            .collect(collector.apply(f -> returnWithDelay(42L, Duration.ofMillis(BLOCKING_MILLIS))))
            .join();
    }
}
