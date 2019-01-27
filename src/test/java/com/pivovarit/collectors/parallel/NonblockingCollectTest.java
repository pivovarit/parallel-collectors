package com.pivovarit.collectors.parallel;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.inParallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToList;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.TimeUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertTimeout;

/**
 * @author Grzegorz Piwowarek
 */
class NonblockingCollectTest extends ExecutorAwareTest {

    @Test
    void shouldReturnImmediatelyCollectionAndNotPolluteExecutor() {
        // given
        executor = threadPoolExecutor(5);

        assertTimeout(ofMillis(50), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, Duration.ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToCollection(ArrayList::new, executor, 42)));
    }

    @Test
    void shouldReturnImmediatelyListAndNotPolluteExecutor() {
        // given
        executor = threadPoolExecutor(5);

        assertTimeout(ofMillis(50), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, Duration.ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToList(executor, 42)));
    }

    @Test
    void shouldReturnImmediatelySetAndNotPolluteExecutor() {
        // given
        executor = threadPoolExecutor(5);

        assertTimeout(ofMillis(50), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, Duration.ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToSet(executor, 42)));
    }
}
