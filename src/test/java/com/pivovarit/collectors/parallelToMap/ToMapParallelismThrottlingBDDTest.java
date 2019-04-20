package com.pivovarit.collectors.parallelToMap;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import com.pivovarit.collectors.infrastructure.TestUtils;
import org.awaitility.Awaitility;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToList;
import static com.pivovarit.collectors.ParallelCollectors.parallelToMap;
import static com.pivovarit.collectors.infrastructure.TestUtils.TRIALS;
import static com.pivovarit.collectors.infrastructure.TestUtils.expectedDuration;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.infrastructure.TestUtils.timed;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * @author Grzegorz Piwowarek
 */
@RunWith(JUnitQuickcheck.class)
public class ToMapParallelismThrottlingBDDTest extends ExecutorAwareTest {

    @Property(trials = TRIALS)
    public void shouldCollectToMapWithThrottledParallelism(@InRange(minInt = 20, maxInt = 100) int unitsOfWork, @InRange(minInt = 1, maxInt = 20) int parallelism) {
        // given
        TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();

        Stream.generate(() -> 42)
          .limit(unitsOfWork)
          .collect(parallelToMap(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), i -> i, executor, parallelism));

        Awaitility.await()
          .until(() -> executor.count() == parallelism);
    }
}
