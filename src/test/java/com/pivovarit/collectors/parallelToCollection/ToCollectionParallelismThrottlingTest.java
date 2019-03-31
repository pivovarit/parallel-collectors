package com.pivovarit.collectors.parallelToCollection;

import com.pivovarit.collectors.infrastructure.TestUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Grzegorz Piwowarek
 */
class ToCollectionParallelismThrottlingTest {

    @Test
    void shouldParallelizeToListAndRespectParallelizm() throws InterruptedException {
        // given
        int parallelism = 2;
        TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();

        CompletableFuture<ArrayList<Long>> result = Stream.generate(() -> supplier(() ->
          returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
          .limit(10)
          .collect(parallelToCollection(ArrayList::new, executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        Awaitility.await()
          .until(() -> executor.count() == parallelism);
    }

    @Test
    void shouldParallelizeToListAndRespectParallelizmMapping() throws InterruptedException {
        // given
        int parallelism = 2;
        TestUtils.CountingExecutor executor = new TestUtils.CountingExecutor();

        CompletableFuture<ArrayList<Long>> result =
          Stream.generate(() -> 42)
            .limit(10)
            .collect(parallelToCollection(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), ArrayList::new, executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        Awaitility.await()
          .until(() -> executor.count() == parallelism);
    }
}
