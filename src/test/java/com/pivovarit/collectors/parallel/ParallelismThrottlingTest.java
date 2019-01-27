package com.pivovarit.collectors.parallel;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.inParallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToList;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.TimeUtils.returnWithDelay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Grzegorz Piwowarek
 */
class ParallelismThrottlingTest {

    @Test
    void shouldParallelizeToListAndRespectParallelizm() {
        // given
        int parallelism = 2;
        int concurrencyLevel = 10;
        CountingExecutor executor = new CountingExecutor();

        CompletableFuture<ArrayList<Long>> result = Stream.generate(() -> supplier(() ->
          returnWithDelay(42L, Duration.ofMillis(Integer.MAX_VALUE))))
          .limit(concurrencyLevel)
          .collect(inParallelToCollection(ArrayList::new, executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        await().until(() -> executor.count() == parallelism);
    }

    @Test
    void shouldParallelizeToSetAndRespectParallelizm() {
        // given
        int parallelism = 2;
        int concurrencyLevel = 10;
        CountingExecutor executor = new CountingExecutor();

        CompletableFuture<List<Long>> result =
          Stream.generate(() -> supplier(() ->
            returnWithDelay(42L, Duration.ofMillis(Integer.MAX_VALUE))))
            .limit(concurrencyLevel)
            .collect(inParallelToList(executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        await().until(() -> executor.count() == parallelism);
    }

    @Test
    void shouldParallelizeToCollectionAndRespectParallelizm() {
        // given
        int parallelism = 2;
        int concurrencyLevel = 10;
        CountingExecutor executor = new CountingExecutor();

        CompletableFuture<Set<Long>> result =
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, Duration.ofMillis(Integer.MAX_VALUE))))
            .limit(concurrencyLevel)
            .collect(inParallelToSet(executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        await().until(() -> executor.count() == parallelism);
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
