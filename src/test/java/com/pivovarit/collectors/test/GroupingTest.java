package com.pivovarit.collectors.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.test.Factory.allGrouping;
import static org.assertj.core.api.Assertions.assertThat;

class GroupingTest {

    @TestFactory
    Stream<DynamicTest> shouldGroupByClassifier() {
        return allGrouping(i -> i % 10, 100)
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              Map<Thread, List<Integer>> threads = new ConcurrentHashMap<>();

              List<Integer> result = Stream.iterate(0, i -> i + 1).limit(30).collect(c.factory().collector(i -> {
                  threads.computeIfAbsent(Thread.currentThread(), (k) -> new ArrayList<>()).add(i);
                  return i;
              }));

              assertThat(threads)
                .hasSizeLessThanOrEqualTo(10)
                .allSatisfy((t, list) -> {
                    assertThat(list.size() % 3).isZero();
                    IntStream.range(0, (list.size() / 3))
                      .mapToObj(i -> list.get(i * 3))
                      .forEachOrdered(s -> assertThat(list).containsSequence(s, s + 10, s + 20));
                });

              assertThat(result)
                .hasSize(30)
                .containsExactlyInAnyOrderElementsOf(IntStream.range(0, 10).boxed()
                  .flatMap(i -> Stream.of(i, i + 10, i + 20))
                  .toList());
          }));
    }
}
