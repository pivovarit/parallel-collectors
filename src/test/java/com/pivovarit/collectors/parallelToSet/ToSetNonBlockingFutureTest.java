package com.pivovarit.collectors.parallelToSet;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * @author Grzegorz Piwowarek
 */
class ToSetNonBlockingFutureTest {

    private final Executor blockingExecutor = i -> {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };

    @Test
    void shouldReturnImmediatelySet() {
        assertTimeoutPreemptively(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(parallelToSet(blockingExecutor, 42)));
    }

    @Test
    void shouldReturnImmediatelySetUnbounded() {
        assertTimeoutPreemptively(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(parallelToSet(blockingExecutor)));
    }

    @Test
    void shouldReturnImmediatelySetMapping() {
        assertTimeoutPreemptively(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(parallelToSet(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), blockingExecutor, 42)));
    }

    @Test
    void shouldReturnImmediatelySetMappingUnbounded() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(parallelToSet(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), blockingExecutor)));
    }
}
