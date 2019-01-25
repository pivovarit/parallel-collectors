package com.pivovarit.collectors;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.After;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.inParallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToList;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

/**
 * @author Grzegorz Piwowarek
 */
@RunWith(JUnitQuickcheck.class)
public class BasicTest {

    private static final int TRIALS = 10;
    private static final int BLOCKING_MILLIS = 200;
    private static final int TIMEOUT = BLOCKING_MILLIS + 150;

    private ExecutorService executor;

    @Property(trials = TRIALS)
    public void shouldCollectToListWithFullParallelism(@InRange(minInt = 100, maxInt = 1000) int collectionSize) {
        // given
        executor = Executors.newFixedThreadPool(collectionSize);

        List<String> result = assertTimeout(Duration.ofMillis(TIMEOUT), () ->
          Stream.generate(() -> supplier(() -> blockingFoo()))
            .limit(collectionSize)
            .collect(inParallelToList(executor))
            .join());

        assertThat(result)
          .hasSize(collectionSize)
          .hasSameSizeAs(new HashSet<>(result));
    }

    @Property(trials = TRIALS)
    public void shouldCollectToSetWithFullParallelism(@InRange(minInt = 100, maxInt = 1000) int collectionSize) {
        // given
        executor = Executors.newFixedThreadPool(collectionSize);

        Set<String> result = assertTimeout(Duration.ofMillis(TIMEOUT), () ->
          Stream.generate(() -> supplier(() -> blockingFoo()))
            .limit(collectionSize)
            .collect(inParallelToSet(executor))
            .join());

        assertThat(result).hasSize(collectionSize);
    }

    @Property(trials = TRIALS)
    public void shouldCollectToCollectionWithFullParallelism(@InRange(minInt = 100, maxInt = 1000) int collectionSize) {
        // given
        executor = Executors.newFixedThreadPool(collectionSize);

        List<String> result = assertTimeout(Duration.ofMillis(TIMEOUT), () ->
          Stream.generate(() -> supplier(() -> blockingFoo()))
            .limit(collectionSize)
            .collect(inParallelToCollection(ArrayList::new, executor))
            .join());

        assertThat(result).hasSize(collectionSize)
          .hasSameSizeAs(new HashSet<>(result));
    }

    @After
    public void after() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    private static String blockingFoo() {
        try {
            Thread.sleep(BLOCKING_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return UUID.randomUUID().toString();
    }
}
