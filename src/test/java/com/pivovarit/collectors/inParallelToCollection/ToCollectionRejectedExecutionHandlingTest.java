package com.pivovarit.collectors.parallelToCollection;

import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.infrastructure.TimeUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Grzegorz Piwowarek
 */
class ToCollectionRejectedExecutionHandlingTest extends ExecutorAwareTest {

    @Test
    void shouldCollectToCollectionAndSurviveRejectedExecutionException() {
        // given
        executor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 1000).boxed()
          .map(i -> supplier(() -> supplier(() -> returnWithDelay(i, ofMillis(10000)))))
          .collect(parallelToCollection(ArrayList::new, executor, 10000))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void shouldCollectToCollectionAndSurviveRejectedExecutionExceptionUnbounded() {
        // given
        executor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 1000).boxed()
          .map(i -> supplier(() -> supplier(() -> returnWithDelay(i, ofMillis(10000)))))
          .collect(parallelToCollection(ArrayList::new, executor))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }


    @Test
    void shouldCollectToCollectionMappingAndSurviveRejectedExecutionException() {
        // given
        executor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 1000).boxed()
          .collect(parallelToCollection(i -> returnWithDelay(i, ofMillis(10000)), ArrayList::new, executor, 10))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void shouldCollectToCollectionMappingAndSurviveRejectedExecutionExceptionUnbounded() {
        // given
        executor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 1000).boxed()
          .collect(parallelToCollection(i -> returnWithDelay(i, ofMillis(10000)), ArrayList::new, executor))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }
}
