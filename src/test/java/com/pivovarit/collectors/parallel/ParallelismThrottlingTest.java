package com.pivovarit.collectors.parallel;

import org.junit.jupiter.api.Test;

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
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Grzegorz Piwowarek
 */
class ParallelismThrottlingTest {

    @Test
    void shouldParallelizeToListAndRespectParallelizm() throws InterruptedException {
        // given
        int parallelism = 2;
        CountingExecutor executor = new CountingExecutor();

        CompletableFuture<ArrayList<Long>> result = Stream.generate(() -> supplier(() ->
          returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
          .limit(10)
          .collect(inParallelToCollection(ArrayList::new, executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        Thread.sleep(50);
        assertThat(executor.count()).isEqualTo(parallelism);
    }

    @Test
    void shouldParallelizeToSetAndRespectParallelizm() throws InterruptedException {
        // given
        int parallelism = 2;
        CountingExecutor executor = new CountingExecutor();

        CompletableFuture<List<Long>> result =
          Stream.generate(() -> supplier(() ->
            returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(10)
            .collect(inParallelToList(executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        Thread.sleep(50);
        assertThat(executor.count()).isEqualTo(parallelism);
    }

    @Test
    void shouldParallelizeToCollectionAndRespectParallelizm() throws InterruptedException {
        // given
        int parallelism = 2;
        CountingExecutor executor = new CountingExecutor();

        CompletableFuture<Set<Long>> result =
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(10)
            .collect(inParallelToSet(executor, parallelism));

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
            .collect(inParallelToCollection(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), ArrayList::new, executor, parallelism));

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
        CountingExecutor executor = new CountingExecutor();

        CompletableFuture<List<Long>> result =
          Stream.generate(() -> 42)
            .limit(10)
            .collect(inParallelToList(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), executor, parallelism));

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
            .collect(inParallelToSet(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), executor, parallelism));

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
