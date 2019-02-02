package com.pivovarit.collectors.parallelToCollection;

import com.pivovarit.collectors.infrastructure.CollectorUtils;
import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.awaitility.core.Supplier;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiFunction;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Grzegorz Piwowarek
 */
class ToCollectionExceptionPropagationTest extends ExecutorAwareTest {
    @TestFactory
    Collection<DynamicTest> dynamicTestsWithCollection() {
        return Arrays.asList(
                DynamicTest.dynamicTest("shouldCollectToCollectionAndNotSwallowException", () -> shouldCollectToCollectionAndNotSwallowException()),
                DynamicTest.dynamicTest("shouldCollectToCollectionAndNotSwallowExceptionUnbounded", () -> shouldCollectToCollectionAndNotSwallowExceptionUnbounded()),
                DynamicTest.dynamicTest("shouldCollectToCollectionMappingAndNotSwallowException", () -> shouldCollectToCollectionMappingAndNotSwallowException()),
                DynamicTest.dynamicTest("shouldCollectToCollectionMappingAndNotSwallowException", () -> shouldCollectToCollectionMappingAndNotSwallowExceptionUnbounded())
        );
    }

    @Test
    void shouldCollectToCollectionAndNotSwallowException() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(() -> IntStream.range(0, 10).boxed()
          .map(i -> supplier(() -> {
              if (i == 7) {
                  throw new IllegalArgumentException();
              } else {
                  return i;
              }
          }))
          .collect(CollectorUtils.<Integer>getCollectorsAsLamdas().get(0).apply(executor, 10))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCollectToCollectionAndNotSwallowExceptionUnbounded() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(() -> IntStream.range(0, 10).boxed()
          .map(i -> supplier(() -> {
              if (i == 7) {
                  throw new IllegalArgumentException();
              } else {
                  return i;
              }
          }))
          .collect(parallelToCollection(ArrayList::new, executor))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }


    @Test
    void shouldCollectToCollectionMappingAndNotSwallowException() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(() -> IntStream.range(0, 10).boxed()
          .collect(parallelToCollection(i -> {
              if (i == 7) {
                  throw new IllegalArgumentException();
              } else {
                  return i;
              }
          }, ArrayList::new, executor, 10))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCollectToCollectionMappingAndNotSwallowExceptionUnbounded() {
        // given
        executor = threadPoolExecutor(10);

        assertThatThrownBy(() -> IntStream.range(0, 10).boxed()
          .collect(parallelToCollection(i -> {
              if (i == 7) {
                  throw new IllegalArgumentException();
              } else {
                  return i;
              }
          }, ArrayList::new, executor))
          .join())
          .isInstanceOf(CompletionException.class)
          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
    }
}
