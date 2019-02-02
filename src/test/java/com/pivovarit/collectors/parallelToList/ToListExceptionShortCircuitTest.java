package com.pivovarit.collectors.parallelToList;

import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToList;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToListExceptionShortCircuitTest extends ExecutorAwareTest {

    @BeforeEach
    void setup() {
        executor = threadPoolExecutor(1);
    }

    @Test
    void shouldCollectToCollectionAndShortCircuitOnException() {

        // given
        LongAdder counter = new LongAdder();

        assertThatThrownBy(() -> {
            IntStream.generate(() -> 42).boxed().limit(10)
              .map(i -> supplier(() -> {
                  counter.increment();
                  throw new IllegalArgumentException();
              }))
              .collect(parallelToList(executor, 1))
              .join();
        }).isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);

        assertThat(counter.longValue()).isOne();
    }

    @Test
    void shouldCollectToCollectionAndShortCircuitOnExceptionUnbounded() {

        // given
        LongAdder counter = new LongAdder();

        assertThatThrownBy(() -> {
            IntStream.generate(() -> 42).boxed().limit(10)
              .map(i -> supplier(() -> {
                  counter.increment();
                  throw new IllegalArgumentException();
              }))
              .collect(parallelToList(executor))
              .join();
        }).isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);

        assertThat(counter.longValue()).isOne();
    }
}
