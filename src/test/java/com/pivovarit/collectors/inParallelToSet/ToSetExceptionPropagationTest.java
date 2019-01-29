package com.pivovarit.collectors.inParallelToSet;

import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.inParallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Grzegorz Piwowarek
 */
class ToSetExceptionPropagationTest extends ExecutorAwareTest {

    @Test
    void shouldCollectToSetAndNotSwallowException() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(() -> {
            IntStream.range(0, 10).boxed()
              .map(i -> supplier(() -> {
                  if (i == 7) {
                      throw new IllegalArgumentException();
                  } else {
                      return i;
                  }
              }))
              .collect(inParallelToSet(executor, 10))
              .join();
        })
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCollectToSetAndNotSwallowExceptionUnbounded() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(() -> {
            IntStream.range(0, 10).boxed()
              .map(i -> supplier(() -> {
                  if (i == 7) {
                      throw new IllegalArgumentException();
                  } else {
                      return i;
                  }
              }))
              .collect(inParallelToSet(executor))
              .join();
        })
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCollectToSetMappingAndNotSwallowException() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(() -> {
            IntStream.range(0, 10).boxed()
              .collect(inParallelToSet(i -> {
                  if (i == 7) {
                      throw new IllegalArgumentException();
                  } else {
                      return i;
                  }
              }, executor, 10))
              .join();
        })
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCollectToSetMappingAndNotSwallowExceptionUnbounded() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(() -> {
            IntStream.range(0, 10).boxed()
              .collect(inParallelToSet(i -> {
                  if (i == 7) {
                      throw new IllegalArgumentException();
                  } else {
                      return i;
                  }
              }, executor))
              .join();
        })
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }
}
