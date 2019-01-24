package com.pivovarit.collectors;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.After;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.inParallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToList;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

/**
 * @author Grzegorz Piwowarek
 */
@RunWith(JUnitQuickcheck.class)
public class ParallelCollectorsParallelismTest {

    private static final int TRIALS = 10;
    private static final int BLOCKING_MILLIS = 200;

    private ExecutorService executor;

    @Property(trials = TRIALS)
    public void shouldCollectToListWithThrottledParallelism(@InRange(minInt = 2, maxInt = 10) int collectionSize) {
        // given
        executor = Executors.newFixedThreadPool(collectionSize);
        int expectedDuration = collectionSize * BLOCKING_MILLIS;

        long duration = time(() -> {
            Stream.generate(() -> supplier(() -> blockingFoo()))
              .limit(collectionSize)
              .collect(inParallelToList(executor, 1))
              .join();
        });

        assertThat(duration)
          .isGreaterThanOrEqualTo(expectedDuration)
          .isCloseTo(expectedDuration, withPercentage(20));
    }

    @Property(trials = TRIALS)
    public void shouldCollectToSetWithThrottledParallelism(@InRange(minInt = 2, maxInt = 10) int collectionSize) {
        // given
        executor = Executors.newFixedThreadPool(collectionSize);
        int expectedDuration = collectionSize * BLOCKING_MILLIS;

        long duration = time(() -> {
            Stream.generate(() -> supplier(() -> blockingFoo()))
              .limit(collectionSize)
              .collect(inParallelToSet(executor, 1))
              .join();
        });

        assertThat(duration)
          .isGreaterThanOrEqualTo(expectedDuration)
          .isCloseTo(collectionSize * BLOCKING_MILLIS, withPercentage(20));
    }

    @Property(trials = TRIALS)
    public void shouldCollectToCollectionWithThrottledParallelism(@InRange(minInt = 2, maxInt = 10) int collectionSize) {
        // given
        executor = Executors.newFixedThreadPool(collectionSize);
        int expectedDuration = collectionSize * BLOCKING_MILLIS;

        long duration = time(() -> {
            Stream.generate(() -> supplier(() -> blockingFoo()))
              .limit(collectionSize)
              .collect(inParallelToCollection(ArrayList::new, executor, 1))
              .join();
        });

        assertThat(duration)
          .isGreaterThanOrEqualTo(expectedDuration)
          .isCloseTo(collectionSize * BLOCKING_MILLIS, withPercentage(20));
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

    private static long time(Runnable runnable) {
        Instant start = Instant.now();
        runnable.run();
        Instant end = Instant.now();
        return Duration.between(start, end).toMillis();
    }
}
