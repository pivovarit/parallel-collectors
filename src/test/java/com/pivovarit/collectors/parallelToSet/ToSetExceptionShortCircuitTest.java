package com.pivovarit.collectors.parallelToSet;

import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static org.assertj.core.api.Assertions.assertThat;

class ToSetExceptionShortCircuitTest extends ExecutorAwareTest {

    @Test
    void shouldCollectToCollectionAndShortCircuitOnException() {

        // given
        executor = threadPoolExecutor(1);
        LongAdder counter = new LongAdder();

        try {
            IntStream.generate(() -> 42).boxed().limit(10)
              .map(i -> supplier(() -> {
                  counter.increment();
                  throw new IllegalArgumentException();
              }))
              .collect(parallelToSet(executor, 1))
              .join();
        } catch (Exception e) {
        }

        assertThat(counter.longValue()).isOne();
    }

    @Test
    void shouldCollectToCollectionAndShortCircuitOnExceptionUnbounded() {

        // given
        executor = threadPoolExecutor(1);
        LongAdder counter = new LongAdder();

        try {
            IntStream.generate(() -> 42).boxed().limit(10)
              .map(i -> supplier(() -> {
                  counter.increment();
                  throw new IllegalArgumentException();
              }))
              .collect(parallelToSet(executor))
              .join();
        } catch (Exception e) {
        }

        assertThat(counter.longValue()).isOne();
    }
}
