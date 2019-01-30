package com.pivovarit.collectors.parallelToSet;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TimeUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertTimeout;

/**
 * @author Grzegorz Piwowarek
 */
class ToSetNonBlockingFutureTest {

    @Test
    void shouldReturnImmediatelySet() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(parallelToSet(Runnable::run, 42)));
    }

    @Test
    void shouldReturnImmediatelySetUnbounded() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(parallelToSet(Runnable::run)));
    }

    @Test
    void shouldReturnImmediatelySetMapping() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(parallelToSet(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), Runnable::run, 42)));
    }

    @Test
    void shouldReturnImmediatelySetMappingUnbounded() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(parallelToSet(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), Runnable::run)));
    }
}
