package com.pivovarit.collectors.parallelToMap;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToMap;
import static com.pivovarit.collectors.infrastructure.TestUtils.TRIALS;
import static com.pivovarit.collectors.infrastructure.TestUtils.expectedDuration;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.infrastructure.TestUtils.timed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * @author Grzegorz Piwowarek
 */
@RunWith(JUnitQuickcheck.class)
public class ToMapParallelismThrottlingBDDTest extends ExecutorAwareTest {

    private static final long BLOCKING_MILLIS = 50;
    private static final long CONSTANT_DELAY = 100;

    @Property(trials = TRIALS)
    public void shouldCollectToMapWithThrottledParallelism(@InRange(minInt = 2, maxInt = 20) int unitsOfWork, @InRange(minInt = 1, maxInt = 40) int parallelism) {
        // given
        executor = threadPoolExecutor(unitsOfWork);
        long expectedDuration = expectedDuration(parallelism, unitsOfWork, BLOCKING_MILLIS);

        Map.Entry<Map<Long, Long>, Long> result = timed(collectWith(f -> parallelToMap(f, f, executor, parallelism), unitsOfWork));

        assertThat(result)
          .satisfies(e -> {
              assertThat(e.getValue())
                .isGreaterThanOrEqualTo(expectedDuration)
                .isCloseTo(expectedDuration, offset(CONSTANT_DELAY));

              assertThat(e.getKey()).hasSize(unitsOfWork);
          });
    }

    private static <R extends Map<Long, Long>> Supplier<R> collectWith(Function<UnaryOperator<Long>, Collector<Long, ?, CompletableFuture<R>>> collector, int unitsOfWork) {
        return () -> Stream.generate(() -> 42L)
          .limit(unitsOfWork)
          .collect(collector.apply(f -> returnWithDelay(42L, Duration.ofMillis(BLOCKING_MILLIS))))
          .join();
    }
}
