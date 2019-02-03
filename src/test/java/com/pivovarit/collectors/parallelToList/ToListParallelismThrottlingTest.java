package com.pivovarit.collectors.parallelToList;

import com.pivovarit.collectors.infrastructure.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToList;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Grzegorz Piwowarek
 */
class ToListParallelismThrottlingTest {

    @Test
    void shouldParallelizeToSetAndRespectParallelizm() throws InterruptedException {
        // given
        int parallelism = 2;
        TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();

        CompletableFuture<List<Long>> result =
          Stream.generate(() -> supplier(() ->
            returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(10)
            .collect(parallelToList(executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        Thread.sleep(50);
        assertThat(executor.count()).isEqualTo(parallelism);
    }


    @Test
    void shouldParallelizeToSetAndRespectParallelizmMapping() throws InterruptedException {
        // given
        int parallelism = 2;
        TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();


        CompletableFuture<List<Long>> result =
          Stream.generate(() -> 42)
            .limit(10)
            .collect(parallelToList(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        Thread.sleep(50);
        assertThat(executor.count()).isEqualTo(parallelism);
    }
}
