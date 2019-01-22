package com.pivovarit.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static com.pivovarit.utils.ParallelCollectors.supplier;
import static com.pivovarit.utils.ParallelCollectors.toListInParallel;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class ParallelCollectionCollectorTest {

    private ExecutorService executor = Executors.newFixedThreadPool(200);

    @Test
    void shouldExecuteWithFullParallelism() {
        assertTimeout(Duration.ofMillis(1050), () ->
          Stream.generate(() -> supplier(() -> blockingFoo()))
            .limit(200)
            .collect(toListInParallel(executor))
            .join());
    }

    @AfterEach
    void after() {
        executor.shutdown();
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
