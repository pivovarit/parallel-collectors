package com.pivovarit.collectors.parallelToCollection;

import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Grzegorz Piwowarek
 */
class ToCollectionExceptionPropagationTest extends ExecutorAwareTest {

    @Test
    void shouldCollectToCollectionAndNotSwallowException() {
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
              .collect(parallelToCollection(ArrayList::new, executor, 10))
              .join();
        })
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCollectToCollectionAndNotSwallowExceptionUnbounded() {
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
              .collect(parallelToCollection(ArrayList::new, executor))
              .join();
        })
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }


    @Test
    void shouldCollectToCollectionMappingAndNotSwallowException() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(() -> {
            IntStream.range(0, 10).boxed()
              .collect(parallelToCollection(i -> {
                  if (i == 7) {
                      throw new IllegalArgumentException();
                  } else {
                      return i;
                  }
              }, ArrayList::new, executor, 10))
              .join();
        })
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCollectToCollectionMappingAndNotSwallowExceptionUnbounded() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(() -> {
            IntStream.range(0, 10).boxed()
              .collect(parallelToCollection(i -> {
                  if (i == 7) {
                      throw new IllegalArgumentException();
                  } else {
                      return i;
                  }
              }, ArrayList::new, executor))
              .join();
        })
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }
}
