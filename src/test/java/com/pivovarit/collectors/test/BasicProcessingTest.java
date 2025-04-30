package com.pivovarit.collectors.test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.test.Factory.all;
import static com.pivovarit.collectors.test.Factory.allOrdered;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;

class BasicProcessingTest {

    @TestFactory
    Stream<DynamicTest> shouldProcessEmpty() {
        return all()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              assertThat(Stream.<Integer>empty().collect(c.factory().collector(i -> i))).isEmpty();
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldProcessAllElements() {
        return all()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var list = IntStream.range(0, 100).boxed().toList();
              List<Integer> result = list.stream().collect(c.factory().collector(i -> i));
              assertThat(result).containsExactlyInAnyOrderElementsOf(list);
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldProcessAllElementsInOrder() {
        return allOrdered()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var list = IntStream.range(0, 100).boxed().toList();
              List<Integer> result = list.stream().collect(c.factory().collector(i -> i));
              assertThat(result).containsExactlyElementsOf(list);
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldStartProcessingImmediately() {
        return all()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var counter = new AtomicInteger();

              Thread.startVirtualThread(() -> {
                  var ignored = Stream.iterate(0, i -> i + 1)
                    .limit(100)
                    .collect(c.factory().collector(i -> returnWithDelay(counter.incrementAndGet(), ofSeconds(1))));
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
                .collect(c.factory().collector(i -> {
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
}
