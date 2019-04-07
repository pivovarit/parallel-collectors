package com.pivovarit.collectors.parallelToMap;

import com.pivovarit.collectors.infrastructure.TestUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToMap;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Grzegorz Piwowarek
 */
class ToMapParallelismThrottlingTest {

    @Test
    void shouldParallelizeToMapAndRespectParallelizm() throws InterruptedException {
        // given
        int parallelism = 2;
        TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();

        CompletableFuture<Map<Long, Long>> result =
          Stream.generate(() -> 42)
            .limit(10)
            .collect(parallelToMap(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        Awaitility.await()
          .until(() -> executor.count() == parallelism);
    }


    @Test
    void shouldParallelizeToMapAndRespectParallelizmMapping() throws InterruptedException {
        // given
        int parallelism = 2;
        TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();


        CompletableFuture<Map<Long, Long>> result =
          Stream.generate(() -> 42)
            .limit(10)
            .collect(parallelToMap(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)),i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)) , executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        Awaitility.await()
          .until(() -> executor.count() == parallelism);
    }
}
