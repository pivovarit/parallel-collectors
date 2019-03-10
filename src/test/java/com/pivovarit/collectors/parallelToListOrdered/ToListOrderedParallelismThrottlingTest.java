package com.pivovarit.collectors.parallelToListOrdered;

import com.pivovarit.collectors.infrastructure.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToList;
import static com.pivovarit.collectors.ParallelCollectors.parallelToListOrdered;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Grzegorz Piwowarek
 */
class ToListOrderedParallelismThrottlingTest {

    @Test
    void shouldParallelizeOrderedToSetAndRespectParallelizmMapping() throws InterruptedException {
        // given
        int parallelism = 2;
        TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();


        CompletableFuture<List<Long>> result =
          Stream.generate(() -> 42)
            .limit(10)
            .collect(parallelToListOrdered(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        Thread.sleep(50);
        assertThat(executor.count()).isEqualTo(parallelism);
    }
}
