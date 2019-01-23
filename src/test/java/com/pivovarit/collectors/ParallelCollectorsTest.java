package com.pivovarit.collectors;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.After;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.ParallelCollectors.toCollectionInParallel;
import static com.pivovarit.collectors.ParallelCollectors.toListInParallel;
import static com.pivovarit.collectors.ParallelCollectors.toSetInParallel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

/**
 * @author Grzegorz Piwowarek
 */
@RunWith(JUnitQuickcheck.class)
public class ParallelCollectorsTest {

    private static final int TRIALS = 10;
    private static final int BLOCKING_MILLIS = 200;
    private static final int TIMEOUT = (int) (BLOCKING_MILLIS * 1.5);

    private ExecutorService executor;

    @Property(trials = TRIALS)
    public void shouldCollectToListWithFullParallelism(@InRange(minInt = 10, maxInt = 100) int collectionSize) {
        // given
        executor = Executors.newFixedThreadPool(collectionSize);

        List<Integer> result = assertTimeout(Duration.ofMillis(TIMEOUT), () ->
          Stream.generate(() -> supplier(() -> blockingFoo()))
            .limit(collectionSize)
            .collect(toListInParallel(executor))
            .join());

        assertThat(result).hasSize(collectionSize);
    }

    @Property(trials = TRIALS)
    public void shouldCollectToSetWithFullParallelism(@InRange(minInt = 10, maxInt = 100) int collectionSize) {
        // given
        executor = Executors.newFixedThreadPool(collectionSize);

        Set<Integer> result = assertTimeout(Duration.ofMillis(TIMEOUT), () ->
          Stream.generate(() -> supplier(() -> blockingFoo()))
            .limit(collectionSize)
            .collect(toSetInParallel(executor))
            .join());

        assertThat(result).hasSize(1);
    }

    @Property(trials = TRIALS)
    public void shouldCollectToCollectionWithFullParallelism(@InRange(minInt = 10, maxInt = 100) int collectionSize) {
        // given
        executor = Executors.newFixedThreadPool(collectionSize);

        List<Integer> result = assertTimeout(Duration.ofMillis(TIMEOUT), () ->
          Stream.generate(() -> supplier(() -> blockingFoo()))
            .limit(collectionSize)
            .collect(toCollectionInParallel(ArrayList::new, executor))
            .join());

        assertThat(result).hasSize(collectionSize);
    }

    @After
    public void after() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    private static int blockingFoo() {
        try {
            Thread.sleep(BLOCKING_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return 42;
    }
}
