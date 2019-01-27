package com.pivovarit.collectors.parallel;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
@RunWith(JUnitQuickcheck.class)
public class ParallelismThrottlingTest extends ExecutorAwareTest {

    @Property(trials = 5)
    public void shouldParallelizeToListAndRespectParallelizm(@InRange(minInt = 11, maxInt = 20) int concurrencyLevel) {
        // given
        int parallelism = 2;
        executor = threadPoolExecutor(parallelism);

        CompletableFuture<ArrayList<Long>> result = Stream.generate(() -> supplier(() ->
          returnWithDelay(42L, Duration.ofMillis(Integer.MAX_VALUE))))
            .limit(concurrencyLevel)
            .collect(inParallelToCollection(ArrayList::new, executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        await().until(() -> executor.getActiveCount(), i -> i == parallelism);
    }


    @Property(trials = 5)
    public void shouldParallelizeToSetAndRespectParallelizm(@InRange(minInt = 11, maxInt = 20) int concurrencyLevel) {
        // given
        int parallelism = 2;
        executor = threadPoolExecutor(concurrencyLevel);

        CompletableFuture<List<Long>> result =
          Stream.generate(() -> supplier(() ->
            returnWithDelay(42L, Duration.ofMillis(Integer.MAX_VALUE))))
            .limit(concurrencyLevel)
            .collect(inParallelToList(executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        await().until(() -> executor.getActiveCount(), i -> i == parallelism);
    }

    @Property(trials = 5)
    public void shouldParallelizeToCollectionAndRespectParallelizm(@InRange(minInt = 11, maxInt = 20) int concurrencyLevel) {
        // given
        int parallelism = 2;
        executor = threadPoolExecutor(concurrencyLevel);

        CompletableFuture<Set<Long>> result =
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, Duration.ofMillis(Integer.MAX_VALUE))))
          .limit(concurrencyLevel)
          .collect(inParallelToSet(executor, parallelism));

        assertThat(result)
          .isNotCompleted()
          .isNotCancelled();

        await().until(() -> executor.getActiveCount(), i -> i == parallelism);
    }
}
