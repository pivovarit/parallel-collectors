package com.pivovarit.collectors.parallelToCollection;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TimeUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * @author Grzegorz Piwowarek
 */
class ToCollectionNonBlockingFutureTest {

    private final Executor blockingExecutor = i -> {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };

    @Test
    void shouldReturnImmediatelyCollection() {
        assertTimeoutPreemptively(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(parallelToCollection(ArrayList::new, blockingExecutor, 42)));
    }

    @Test
    void shouldReturnImmediatelyCollectionUnbounded() {
        assertTimeoutPreemptively(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(parallelToCollection(ArrayList::new, blockingExecutor)));
    }

    @Test
    void shouldReturnImmediatelyCollectionMapping() {
        assertTimeoutPreemptively(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(parallelToCollection(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), ArrayList::new, blockingExecutor, 42)));
    }

    @Test
    void shouldReturnImmediatelyCollectionMappingUnbounded() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(parallelToCollection(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), ArrayList::new, blockingExecutor)));
    }


}
