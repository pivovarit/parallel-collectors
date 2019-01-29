package com.pivovarit.collectors.inParallelToList;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.inParallelToList;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TimeUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertTimeout;

/**
 * @author Grzegorz Piwowarek
 */
class ToListNonBlockingFutureTest {

    @Test
    void shouldReturnImmediatelyList() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToList(Runnable::run, 42)));
    }

    @Test
    void shouldReturnImmediatelyListUnbounded() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToList(Runnable::run)));
    }

    @Test
    void shouldReturnImmediatelyListMapping() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(inParallelToList(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), Runnable::run, 42)));
    }

    @Test
    void shouldReturnImmediatelyListMappingUnbounded() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(inParallelToList(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), Runnable::run)));
    }
}
