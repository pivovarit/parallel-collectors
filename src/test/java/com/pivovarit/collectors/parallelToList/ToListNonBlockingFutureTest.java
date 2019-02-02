package com.pivovarit.collectors.parallelToList;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToList;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TimeUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * @author Grzegorz Piwowarek
 */
class ToListNonBlockingFutureTest {

    private final Executor blockingExecutor = i -> {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };

    @Test
    void shouldReturnImmediatelyList() {
        assertTimeoutPreemptively(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(parallelToList(blockingExecutor, 42)));
    }

    @Test
    void shouldReturnImmediatelyListUnbounded() {
        assertTimeoutPreemptively(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(parallelToList(blockingExecutor)));
    }

    @Test
    void shouldReturnImmediatelyListMapping() {
        assertTimeoutPreemptively(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(parallelToList(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), blockingExecutor, 42)));
    }

    @Test
    void shouldReturnImmediatelyListMappingUnbounded() {
        assertTimeoutPreemptively(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(parallelToList(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), blockingExecutor)));
    }
}
