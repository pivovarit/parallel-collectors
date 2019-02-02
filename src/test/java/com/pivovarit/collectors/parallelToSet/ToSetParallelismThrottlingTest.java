package com.pivovarit.collectors.parallelToSet;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Grzegorz Piwowarek
 */
class ToSetParallelismThrottlingTest {

    @Test
    void shouldParallelizeToCollectionAndRespectParallelizm() throws InterruptedException {
        // given
        int parallelism = 2;
        CountingExecutor executor = new CountingExecutor();

        CompletableFuture<Set<Long>> result =
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(10)
            .collect(parallelToSet(executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        Thread.sleep(50);
        assertThat(executor.count()).isEqualTo(parallelism);
    }

    @Test
    void shouldParallelizeToCollectionAndRespectParallelizmMapping() throws InterruptedException {
        // given
        int parallelism = 2;
        CountingExecutor executor = new CountingExecutor();

        CompletableFuture<Set<Long>> result =
          Stream.generate(() -> 42)
            .limit(10)
            .collect(parallelToSet(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), executor, parallelism));

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
