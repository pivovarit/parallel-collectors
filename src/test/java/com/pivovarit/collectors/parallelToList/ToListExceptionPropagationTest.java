package com.pivovarit.collectors.parallelToList;

import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToList;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Grzegorz Piwowarek
 */
class ToListExceptionPropagationTest extends ExecutorAwareTest {

    @Test
    void shouldCollectToListAndNotSwallowException() {
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
              .collect(parallelToList(executor, 10))
              .join();
        })
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCollectToListAndNotSwallowExceptionUnbounded() {
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
              .collect(parallelToList(executor))
              .join();
        })
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCollectToListMappingAndNotSwallowException() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(() -> {
            IntStream.range(0, 10).boxed()
              .collect(parallelToList(i -> {
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
    void shouldCollectToListMappingAndNotSwallowExceptionUnbounded() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(() -> {
            IntStream.range(0, 10).boxed()
              .collect(parallelToList(i -> {
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
