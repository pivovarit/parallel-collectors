package com.pivovarit.collectors.parallel;

import com.pivovarit.collectors.infrastructure.TestUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallel;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Grzegorz Piwowarek
 */
class ParallelismThrottlingTest {

    @Test
    void shouldParallelizeToSetAndRespectParallelizm() {
        // given
        int parallelism = 2;
        TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();

        CompletableFuture<List<Long>> result =
          Stream.generate(() -> 42L)
            .limit(10)
            .collect(parallel(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), toList(), executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        await()
          .pollDelay(500, TimeUnit.MILLISECONDS)
          .until(() -> executor.count() == parallelism);
    }


    @Test
    void shouldParallelizeToSetAndRespectParallelizmMapping() {
        // given
        int parallelism = 2;
        TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();


        CompletableFuture<List<Long>> result =
          Stream.generate(() -> 42)
            .limit(10)
            .collect(parallel(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), toList(), executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        await()
          .until(() -> executor.count() == parallelism);
    }
}
