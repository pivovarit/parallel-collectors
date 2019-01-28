package com.pivovarit.collectors.parallel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.inParallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToList;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Grzegorz Piwowarek
 */
class RejectedExecutionHandlingTest extends ExecutorAwareTest {

    @Test
    void shouldCollectToCollectionAndSurviveRejectedExecutionException() {
        // given
        executor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 10).boxed()
          .map(i -> supplier(() -> i))
          .collect(inParallelToCollection(ArrayList::new, executor, 10))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void shouldCollectToListAndSurviveRejectedExecutionException() {
        // given
        executor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 10).boxed()
          .map(i -> supplier(() -> i))
          .collect(inParallelToList(executor, 10))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void shouldCollectToSetAndSurviveRejectedExecutionException() {
        // given
        executor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 10).boxed()
          .map(i -> supplier(() -> i))
          .collect(inParallelToSet(executor, 10))
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

        assertThatThrownBy(() -> IntStream.range(0, 10).boxed()
          .collect(inParallelToCollection(i -> i, ArrayList::new, executor, 10))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void shouldCollectToListMappingAndSurviveRejectedExecutionException() {
        // given
        executor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 10).boxed()
          .collect(inParallelToList(i -> i, executor, 10))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void shouldCollectToSetMappingAndSurviveRejectedExecutionException() {
        // given
        executor = new ThreadPoolExecutor(1, 1,
          0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
        );

        assertThatThrownBy(() -> IntStream.range(0, 10).boxed()
          .collect(inParallelToSet(i -> i, executor, 10))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(RejectedExecutionException.class);
    }
}
