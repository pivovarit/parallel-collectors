package com.pivovarit.collectors.inParallelToCollection;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.inParallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TimeUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertTimeout;

/**
 * @author Grzegorz Piwowarek
 */
class ToCollectionNonBlockingFutureTest {

    @Test
    void shouldReturnImmediatelyCollection() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToCollection(ArrayList::new, Runnable::run, 42)));
    }

    @Test
    void shouldReturnImmediatelyCollectionUnbounded() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToCollection(ArrayList::new, Runnable::run)));
    }

    @Test
    void shouldReturnImmediatelyCollectionMapping() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(inParallelToCollection(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), ArrayList::new, Runnable::run, 42)));
    }

    @Test
    void shouldReturnImmediatelyCollectionMappingUnbounded() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(inParallelToCollection(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), ArrayList::new, Runnable::run)));
    }
}
