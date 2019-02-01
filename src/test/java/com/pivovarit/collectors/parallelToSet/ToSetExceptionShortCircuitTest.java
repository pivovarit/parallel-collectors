package com.pivovarit.collectors.parallelToSet;

import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class ToSetExceptionShortCircuitTest extends ExecutorAwareTest {

    @Test
    void shouldCollectToCollectionAndShortCircuitOnException() {

        // given
        executor = threadPoolExecutor(1);

        assertTimeoutPreemptively(Duration.ofMillis(500), () -> {
            assertThatThrownBy(() -> {
                IntStream.generate(() -> 42).boxed()
                  .map(i -> supplier(() -> {
                      try {
                          Thread.sleep(100);
                      } catch (InterruptedException e) {
                          throw new IllegalStateException(e);
                      }
                      throw new IllegalArgumentException();
                  }))
                  .collect(parallelToSet(executor, 1))
                  .join();
            })
              .isInstanceOf(CompletionException.class)
              .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void shouldCollectToCollectionAndShortCircuitOnExceptionUnbounded() {

        // given
        executor = threadPoolExecutor(1);

        assertTimeoutPreemptively(Duration.ofMillis(500), () -> {
            assertThatThrownBy(() -> {
                IntStream.generate(() -> 42).boxed()
                  .map(i -> supplier(() -> {
                      try {
                          Thread.sleep(100);
                      } catch (InterruptedException e) {
                          throw new IllegalStateException(e);
                      }
                      throw new IllegalArgumentException();
                  }))
                  .collect(parallelToSet(executor))
                  .join();
            })
              .isInstanceOf(CompletionException.class)
              .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
        });
    }
}
