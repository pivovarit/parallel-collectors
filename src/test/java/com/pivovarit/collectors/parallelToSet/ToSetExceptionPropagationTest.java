package com.pivovarit.collectors.parallelToSet;

import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToSet;
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

        assertThatThrownBy(IntStream.range(0, 10).boxed()
          .map(i -> supplier(() -> throwing(i)))
          .collect(parallelToSet(executor, 10))::join)
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCollectToSetAndNotSwallowExceptionUnbounded() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(IntStream.range(0, 10).boxed()
          .map(i -> supplier(() -> throwing(i)))
          .collect(parallelToSet(executor))::join)
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCollectToSetMappingAndNotSwallowException() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(IntStream.range(0, 10).boxed()
          .collect(parallelToSet(this::throwing, executor, 10))::join)
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCollectToSetMappingAndNotSwallowExceptionUnbounded() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(IntStream.range(0, 10).boxed()
          .collect(parallelToSet(this::throwing, executor))::join)
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    private Integer throwing(Integer i) {
        if (i == 7) {
            throw new IllegalArgumentException();
        } else {
            return i;
        }
    }
}
