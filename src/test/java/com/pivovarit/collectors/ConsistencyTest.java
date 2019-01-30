package com.pivovarit.collectors;

import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class ConsistencyTest extends ExecutorAwareTest {

    @Test
    void shouldNotDeadlockOnQueueAdd() {
        executor = threadPoolExecutor(10);

        assertTimeout(Duration.ofSeconds(2), () -> {
            try {
                IntStream.range(0, 10).boxed()
                  .collect(new ThrottlingParallelCollector<Integer, Integer, List<Integer>>((Integer i) -> i, ArrayList::new, executor, 10, new ConcurrentLinkedQueue<>(),
                    new LinkedList<CompletableFuture<Integer>>() {
                        @Override
                        public boolean offer(CompletableFuture<Integer> integerSupplier) {
                            return false;
                        }

                        @Override
                        public boolean add(CompletableFuture<Integer> integerCompletableFuture) {
                            throw new IllegalStateException();
                        }
                    }))
                  .join();
            } catch (Exception ignored) { }
        });
    }

    @Test
    void shouldNotDeadlockOnQueueAddUnbounded() {
        executor = threadPoolExecutor(10);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            try {
                IntStream.range(0, 10).boxed()
                  .collect(new UnboundedParallelCollector<Integer, Integer, List<Integer>>((Integer i) -> i, ArrayList::new, executor, new ConcurrentLinkedQueue<>(),
                    new LinkedList<CompletableFuture<Integer>>() {
                        @Override
                        public boolean offer(CompletableFuture<Integer> integerSupplier) {
                            return false;
                        }

                        @Override
                        public boolean add(CompletableFuture<Integer> integerCompletableFuture) {
                            throw new IllegalStateException();
                        }
                    }))
                  .join();
            } catch (Exception ignored) { }
        });
    }
}
