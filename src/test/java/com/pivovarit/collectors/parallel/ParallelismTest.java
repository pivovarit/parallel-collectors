package com.pivovarit.collectors.parallel;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.inParallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToList;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.TimeUtils.timed;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Grzegorz Piwowarek
 */
@RunWith(JUnitQuickcheck.class)
public class ParallelismTest {

    private static final long BLOCKING_MILLIS = 50;
    private static final long CONSTANT_DELAY = 75;

    private ExecutorService executor;

    @Property
    public void shouldCollectToListWithThrottledParallelism(@InRange(minInt = 2 , maxInt = 20) int unitsOfWork, @InRange(minInt = 1, maxInt = 40) int parallelism) {
        // given
        executor = Executors.newFixedThreadPool(unitsOfWork);
        long expectedDuration = expectedDuration(parallelism, unitsOfWork);

        assertThat(timed(collectWith(inParallelToList(executor, parallelism), unitsOfWork)))
          .isGreaterThanOrEqualTo(expectedDuration)
          .isCloseTo(expectedDuration, Offset.offset(CONSTANT_DELAY));
    }

    @Property
    public void shouldCollectToSetWithThrottledParallelism(@InRange(minInt = 2, maxInt = 20) int unitsOfWork, @InRange(minInt = 1, maxInt = 40) int parallelism) {
        // given
        executor = Executors.newFixedThreadPool(unitsOfWork);
        long expectedDuration = expectedDuration(parallelism, unitsOfWork);

        assertThat(timed(collectWith(inParallelToSet(executor, parallelism), unitsOfWork)))
          .isGreaterThanOrEqualTo(expectedDuration)
          .isCloseTo(expectedDuration, Offset.offset(CONSTANT_DELAY));
    }

    @Property
    public void shouldCollectToCollectionWithThrottledParallelism(@InRange(minInt = 2, maxInt = 20) int unitsOfWork, @InRange(minInt = 1, maxInt = 40) int parallelism) {
        // given
        executor = Executors.newFixedThreadPool(unitsOfWork);
        long expectedDuration = expectedDuration(parallelism, unitsOfWork);

        assertThat(timed(collectWith(inParallelToCollection(ArrayList::new, executor, parallelism), unitsOfWork)))
          .isGreaterThanOrEqualTo(expectedDuration)
          .isCloseTo(expectedDuration, Offset.offset(CONSTANT_DELAY));
    }

    @After
    public void after() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    private static Long sleep() {
        try {
            Thread.sleep(ParallelismTest.BLOCKING_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return 42L;
    }

    private static long expectedDuration(long parallelism, long unitsOfWork) {
        if (unitsOfWork < parallelism) {
            return BLOCKING_MILLIS;
        } else if (unitsOfWork % parallelism == 0) {
            return (unitsOfWork / parallelism) * BLOCKING_MILLIS;
        } else {
            return (unitsOfWork / parallelism + 1) * BLOCKING_MILLIS;
        }
    }

    private static <T, R extends Collection<T>> Runnable collectWith(Collector<Supplier<Long>, List<CompletableFuture<T>>, CompletableFuture<R>> collector, int unitsOfWork) {
        return () -> Stream.generate(() -> supplier(() -> sleep()))
          .limit(unitsOfWork)
          .collect(collector)
          .join();
    }
}
