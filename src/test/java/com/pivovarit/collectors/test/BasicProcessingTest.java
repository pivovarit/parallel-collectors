package com.pivovarit.collectors.test;

import com.pivovarit.collectors.ParallelCollectors;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.pivovarit.collectors.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.test.BasicProcessingTest.CollectorDefinition.collector;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;

class BasicProcessingTest {

    private static Stream<CollectorDefinition<Integer, Integer>> all() {
        return Stream.of(
          collector("parallel()", f -> collectingAndThen(ParallelCollectors.parallel(f), c -> c.join().toList())),
          collector("parallel(e)", f -> collectingAndThen(ParallelCollectors.parallel(f, e()), c -> c.join().toList())),
          collector("parallel(e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, e(), p()), c -> c.join().toList())),
          collector("parallel(toList())", f -> collectingAndThen(ParallelCollectors.parallel(f, toList()), CompletableFuture::join)),
          collector("parallel(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e()), CompletableFuture::join)),
          collector("parallel(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallelToStream()", f -> collectingAndThen(ParallelCollectors.parallelToStream(f), Stream::toList)),
          collector("parallelToStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, e()), Stream::toList)),
          collector("parallelToStream(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, e(), p()), Stream::toList)),
          collector("parallelToStream(e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStream()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f), Stream::toList)),
          collector("parallelToOrderedStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e()), Stream::toList)),
          collector("parallelToOrderedStream(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStream(e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p()), Stream::toList))
        );
    }

    public static Stream<CollectorDefinition<Integer, Integer>> allOrdered() {
        return Stream.of(
          collector("parallel()", f -> collectingAndThen(ParallelCollectors.parallel(f), c -> c.join().toList())),
          collector("parallel(e)", f -> collectingAndThen(ParallelCollectors.parallel(f, e()), c -> c.join().toList())),
          collector("parallel(e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, e(), p()), c -> c.join().toList())),
          collector("parallel(toList())", f -> collectingAndThen(ParallelCollectors.parallel(f, toList()), CompletableFuture::join)),
          collector("parallel(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e()), CompletableFuture::join)),
          collector("parallel(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallelToOrderedStream()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f), Stream::toList)),
          collector("parallelToOrderedStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e()), Stream::toList)),
          collector("parallelToOrderedStream(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStream(e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p()), Stream::toList))
        );
    }

    @TestFactory
    Stream<DynamicTest> shouldProcessEmpty() {
        return all()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              assertThat(Stream.<Integer>empty().collect(c.collector().collector(i -> i))).isEmpty();
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldProcessAllElements() {
        return all()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var list = IntStream.range(0, 100).boxed().toList();
              List<Integer> result = list.stream().collect(c.collector().collector(i -> i));
              assertThat(result).containsExactlyInAnyOrderElementsOf(list);
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldProcessAllElementsInOrder() {
        return allOrdered()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var list = IntStream.range(0, 100).boxed().toList();
              List<Integer> result = list.stream().collect(c.collector().collector(i -> i));
              assertThat(result).containsAnyElementsOf(list);
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldStartProcessingImmediately() {
        return all()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var counter = new AtomicInteger();

              Thread.startVirtualThread(() -> {
                  Stream.iterate(0, i -> i + 1)
                    .limit(100)
                    .collect(c.collector().collector(i -> returnWithDelay(counter.incrementAndGet(), ofSeconds(1))));
              });

              await()
                .pollInterval(1, MILLISECONDS)
                .atMost(500, MILLISECONDS)
                .until(() -> counter.get() > 0);
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldInterruptOnException() {
        return all()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var counter = new AtomicLong();
              int size = 4;
              var latch = new CountDownLatch(size);

              assertThatThrownBy(() -> IntStream.range(0, size).boxed()
                .collect(c.collector().collector(i -> {
                    try {
                        latch.countDown();
                        latch.await();
                        if (i == 0) {
                            throw new NullPointerException();
                        }
                        Thread.sleep(Integer.MAX_VALUE);
                    } catch (InterruptedException ex) {
                        counter.incrementAndGet();
                    }
                    return i;
                })))
                .hasCauseExactlyInstanceOf(NullPointerException.class);

              await().atMost(1, SECONDS).until(() -> counter.get() == size - 1);
          }));
    }

    record CollectorDefinition<T, R>(String name, Factory.CollectorFactory<T, R> collector) {
        static <T, R> CollectorDefinition<T, R> collector(String name, Factory.CollectorFactory<T, R> collector) {
            return new CollectorDefinition<>(name, collector);
        }
    }

    private static Executor e() {
        return Executors.newCachedThreadPool();
    }

    private static int p() {
        return 4;
    }
}
