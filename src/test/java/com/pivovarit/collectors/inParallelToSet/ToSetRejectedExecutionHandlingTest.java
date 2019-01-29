package com.pivovarit.collectors.inParallelToSet;

import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.inParallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TimeUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Grzegorz Piwowarek
 */
class ToSetRejectedExecutionHandlingTest extends ExecutorAwareTest {

    @Test
    void shouldCollectToSetAndSurviveRejectedExecutionException() {
        // given
        executor = new ThreadPoolExecutor(2, 2,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 1000).boxed()
          .map(i -> supplier(() -> supplier(() -> returnWithDelay(i, ofMillis(10000)))))
          .collect(inParallelToSet(executor, 10000))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void shouldCollectToSetAndSurviveRejectedExecutionExceptionUnbounded() {
        // given
        executor = new ThreadPoolExecutor(2, 2,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 1000).boxed()
          .map(i -> supplier(() -> supplier(() -> returnWithDelay(i, ofMillis(10000)))))
          .collect(inParallelToSet(executor))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void shouldCollectToSetMappingAndSurviveRejectedExecutionExceptionUnbounded() {
        // given
        executor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 1000).boxed()
          .collect(inParallelToSet(i -> returnWithDelay(i, ofMillis(10000)), executor))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }
}
