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
class NonblockingCollectTest {

    @Test
    void shouldReturnImmediatelyCollectionAndNotPolluteExecutor() {
        assertTimeout(ofMillis(50), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, Duration.ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToCollection(ArrayList::new, Runnable::run, 42)));
    }

    @Test
    void shouldReturnImmediatelyListAndNotPolluteExecutor() {
        assertTimeout(ofMillis(50), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, Duration.ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToList(Runnable::run, 42)));
    }

    @Test
    void shouldReturnImmediatelySetAndNotPolluteExecutor() {
        assertTimeout(ofMillis(50), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, Duration.ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToSet(Runnable::run, 42)));
    }
}
