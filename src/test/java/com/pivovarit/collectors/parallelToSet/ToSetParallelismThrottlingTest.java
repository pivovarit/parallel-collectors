package com.pivovarit.collectors.parallelToSet;

import com.pivovarit.collectors.infrastructure.TestUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallel;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Grzegorz Piwowarek
 */
class ToSetParallelismThrottlingTest {

    @Test
    void shouldParallelizeToCollectionAndRespectParallelizm() throws InterruptedException {
        // given
        int parallelism = 2;
        TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();


        CompletableFuture<Set<Long>> result =
          Stream.generate(() -> 42L)
            .limit(10)
            .collect(parallel(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), toSet(), executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        Awaitility.await()
          .until(() -> executor.count() == parallelism);
    }

    @Test
    void shouldParallelizeToCollectionAndRespectParallelizmMapping() throws InterruptedException {
        // given
        int parallelism = 2;
        TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();

        CompletableFuture<Set<Long>> result =
          Stream.generate(() -> 42)
            .limit(10)
            .collect(parallel(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), toSet(), executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        Awaitility.await()
          .until(() -> executor.count() == parallelism);
    }
}
