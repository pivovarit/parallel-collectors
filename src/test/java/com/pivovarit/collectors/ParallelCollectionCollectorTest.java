package com.pivovarit.collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.ParallelCollectors.toListInParallel;
import static com.pivovarit.collectors.ParallelCollectors.toSetInParallel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class ParallelCollectionCollectorTest {

    private ExecutorService executor;

    @Test
    void shouldCollectToListWithFullParallelism() {
        // given
        int collectionSize = 200;
        executor = Executors.newFixedThreadPool(collectionSize);

        List<Integer> result = assertTimeout(Duration.ofMillis(1100), () ->
          Stream.generate(() -> supplier(() -> blockingFoo()))
            .limit(collectionSize)
            .collect(toListInParallel(executor))
            .join());

        assertThat(result).hasSize(collectionSize);
    }

    @Test
    void shouldCollectToSetWithFullParallelism() {
        // given
        int collectionSize = 200;
        executor = Executors.newFixedThreadPool(collectionSize);

        Set<Integer> result = assertTimeout(Duration.ofMillis(1100), () ->
          Stream.generate(() -> supplier(() -> blockingFoo()))
            .limit(collectionSize)
            .collect(toSetInParallel(executor))
            .join());

        assertThat(result).hasSize(1);
    }

    @AfterEach
    void after() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    private static int blockingFoo() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return 42;
    }
}
