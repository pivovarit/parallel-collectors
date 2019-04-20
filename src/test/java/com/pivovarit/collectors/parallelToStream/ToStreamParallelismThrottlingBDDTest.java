package com.pivovarit.collectors.parallelToStream;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import com.pivovarit.collectors.infrastructure.TestUtils;
import org.awaitility.Awaitility;
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

import static com.pivovarit.collectors.ParallelCollectors.parallelToList;
import static com.pivovarit.collectors.ParallelCollectors.parallelToStream;
import static com.pivovarit.collectors.infrastructure.TestUtils.TRIALS;
import static com.pivovarit.collectors.infrastructure.TestUtils.expectedDuration;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelayGaussian;
import static com.pivovarit.collectors.infrastructure.TestUtils.timed;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * @author Grzegorz Piwowarek
 */
@RunWith(JUnitQuickcheck.class)
public class ToStreamParallelismThrottlingBDDTest extends ExecutorAwareTest {

    @Property(trials = TRIALS)
    public void shouldCollectToListWithThrottledParallelism(@InRange(minInt = 20, maxInt = 100) int unitsOfWork, @InRange(minInt = 1, maxInt = 20) int parallelism) {
        // given
        TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();

        Stream.generate(() -> 42)
          .limit(unitsOfWork)
          .collect(parallelToStream(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), executor, parallelism));

        Awaitility.await()
          .until(() -> executor.count() == parallelism);
    }

    @Property(trials = TRIALS)
    public void shouldMaintainOrder(@InRange(minInt = 2, maxInt = 20) int unitsOfWork, @InRange(minInt = 2, maxInt = 40) int parallelism) {
        // given
        executor = threadPoolExecutor(unitsOfWork);
        List<Integer> result = Stream.iterate(0, i -> i + 1).limit(20)
          .collect(parallelToStream(i -> returnWithDelayGaussian(i, Duration.ofMillis(10)), executor, parallelism))
          .join()
          .collect(Collectors.toList());

        assertThat(result).isSorted();

        executor.shutdownNow();
    }
}
