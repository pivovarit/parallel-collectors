package com.pivovarit.collectors.parallelToCollection;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.LongAdder;
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
        CountingExecutor executor = new CountingExecutor();

        CompletableFuture<ArrayList<Long>> result = Stream.generate(() -> supplier(() ->
          returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
          .limit(10)
          .collect(parallelToCollection(ArrayList::new, executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        Thread.sleep(50);
        assertThat(executor.count()).isEqualTo(parallelism);
    }

    @Test
    void shouldParallelizeToListAndRespectParallelizmMapping() throws InterruptedException {
        // given
        int parallelism = 2;
        CountingExecutor executor = new CountingExecutor();

        CompletableFuture<ArrayList<Long>> result =
          Stream.generate(() -> 42)
            .limit(10)
            .collect(parallelToCollection(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), ArrayList::new, executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        Thread.sleep(50);
        assertThat(executor.count()).isEqualTo(parallelism);
    }

    public static class CountingExecutor implements Executor {
        private final LongAdder longAdder = new LongAdder();

        @Override
        public void execute(Runnable command) {
            longAdder.increment();
        }

        long count() {
            return longAdder.longValue();
        }
    }
}
