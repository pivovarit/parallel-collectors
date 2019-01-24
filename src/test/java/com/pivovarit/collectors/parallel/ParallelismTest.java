package com.pivovarit.collectors.parallel;

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
public class ParallelismTest {

    private static final int BLOCKING_MILLIS = 100;

    private ExecutorService executor;

    @Property
    public void shouldCollectToListWithThrottledParallelism(@InRange(minInt = 2 , maxInt = 20) int unitsOfWork, @InRange(minInt = 1, maxInt = 40) int parallelism) {
        // given
        executor = Executors.newFixedThreadPool(unitsOfWork);
        long expectedDuration = BLOCKING_MILLIS * expectedDuration(parallelism, unitsOfWork);

        long duration = time(() -> {
            Stream.generate(() -> supplier(() -> blockingFoo()))
              .limit(unitsOfWork)
              .collect(inParallelToList(executor, parallelism))
              .join();
        });

        assertThat(duration)
          .isGreaterThanOrEqualTo(expectedDuration)
          .isCloseTo(expectedDuration, withPercentage(20));
    }

    @Property
    public void shouldCollectToSetWithThrottledParallelism(@InRange(minInt = 2, maxInt = 20) int unitsOfWork, @InRange(minInt = 1, maxInt = 40) int parallelism) {
        // given
        executor = Executors.newFixedThreadPool(unitsOfWork);
        long expectedDuration = BLOCKING_MILLIS * expectedDuration(parallelism, unitsOfWork);

        long duration = time(() -> {
            Stream.generate(() -> supplier(() -> blockingFoo()))
              .limit(unitsOfWork)
              .collect(inParallelToSet(executor, parallelism))
              .join();
        });

        assertThat(duration)
          .isGreaterThanOrEqualTo(expectedDuration)
          .isCloseTo(expectedDuration, withPercentage(20));
    }

    @Property
    public void shouldCollectToCollectionWithThrottledParallelism(@InRange(minInt = 2, maxInt = 20) int unitsOfWork, @InRange(minInt = 1, maxInt = 40) int parallelism) {
        // given
        executor = Executors.newFixedThreadPool(unitsOfWork);
        long expectedDuration = BLOCKING_MILLIS * expectedDuration(parallelism, unitsOfWork);

        long duration = time(() -> {
            Stream.generate(() -> supplier(() -> blockingFoo()))
              .limit(unitsOfWork)
              .collect(inParallelToCollection(ArrayList::new, executor, parallelism))
              .join();
        });

        assertThat(duration)
          .isGreaterThanOrEqualTo(expectedDuration)
          .isCloseTo(expectedDuration, withPercentage(20));
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

    private static long expectedDuration(long parallelism, long unitsOfWork) {
        if (unitsOfWork < parallelism) {
            return 1;
        } else if (unitsOfWork % parallelism == 0) {
            return unitsOfWork / parallelism;
        } else {
            return unitsOfWork / parallelism + 1;
        }
    }
}
