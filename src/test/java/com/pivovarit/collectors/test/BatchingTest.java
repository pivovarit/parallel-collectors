package com.pivovarit.collectors.test;

import com.pivovarit.collectors.ParallelCollectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.test.Factory.GenericCollector.limitedCollector;
import static com.pivovarit.collectors.test.Factory.e;
import static java.time.Duration.ofMillis;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;

class BatchingTest {
  private static Stream<
          Factory.GenericCollector<Factory.CollectorFactoryWithParallelism<Integer, Integer>>>
      allBatching() {
    return Stream.of(
        limitedCollector(
            "parallel(e, p) [batching]",
            (f, p) ->
                collectingAndThen(
                    ParallelCollectors.Batching.parallel(f, e(), p),
                    c -> c.thenApply(Stream::toList).join())),
        limitedCollector(
            "parallel(toList(), e, p) [batching]",
            (f, p) ->
                collectingAndThen(
                    ParallelCollectors.Batching.parallel(f, toList(), e(), p),
                    CompletableFuture::join)),
        limitedCollector(
            "parallelToStream(e, p) [batching]",
            (f, p) ->
                collectingAndThen(
                    ParallelCollectors.Batching.parallelToStream(f, e(), p), Stream::toList)),
        limitedCollector(
            "parallelToOrderedStream(e, p) [batching]",
            (f, p) ->
                collectingAndThen(
                    ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p),
                    Stream::toList)));
  }

  @TestFactory
  Stream<DynamicTest> shouldProcessOnExactlyNThreads() {
    return allBatching()
        .map(
            c ->
                DynamicTest.dynamicTest(
                    c.name(),
                    () -> {
                      var threads = new ConcurrentSkipListSet<>();
                      var parallelism = 4;

                      var ignored =
                          Stream.generate(() -> 42)
                              .limit(100)
                              .collect(
                                  c.factory()
                                      .collector(
                                          i -> {
                                            threads.add(Thread.currentThread().getName());
                                            return i;
                                          },
                                          parallelism));

                      assertThat(threads).hasSizeLessThanOrEqualTo(parallelism);
                    }));
  }

  @Test
  void shouldCollectInCompletionOrder() {
    var result =
        of(300, 200, 0, 400)
            .collect(
                ParallelCollectors.Batching.parallelToStream(
                    i -> returnWithDelay(i, ofMillis(i)),
                    Executors.newVirtualThreadPerTaskExecutor(),
                    2))
            .toList();

    assertThat(result).containsExactly(0, 400, 300, 200);
  }

  @Test
  void shouldCollectInOriginalOrder() {
    var result =
        of(300, 200, 0, 400)
            .collect(
                ParallelCollectors.Batching.parallelToOrderedStream(
                    i -> returnWithDelay(i, ofMillis(i)),
                    Executors.newVirtualThreadPerTaskExecutor(),
                    2))
            .toList();

    assertThat(result).containsExactly(300, 200, 0, 400);
  }
}
