package com.pivovarit.collectors.test;

import com.pivovarit.collectors.Grouped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.test.Factory.allGrouping;
import static com.pivovarit.collectors.test.Factory.allGroupingOrdered;
import static org.assertj.core.api.Assertions.assertThat;

class GroupingTest {

  @TestFactory
  Stream<DynamicTest> shouldGroupByClassifier() {
    return allGrouping(i -> i % 10, 100)
        .map(
            c ->
                DynamicTest.dynamicTest(
                    c.name(),
                    () -> {
                      Map<Thread, List<Integer>> threads = new ConcurrentHashMap<>();

                      List<Grouped<Integer, Integer>> results =
                          Stream.iterate(0, i -> i + 1)
                              .limit(30)
                              .collect(
                                  c.factory()
                                      .collector(
                                          i -> {
                                            threads
                                                .computeIfAbsent(
                                                    Thread.currentThread(),
                                                    (k) -> new ArrayList<>())
                                                .add(i);
                                            return i;
                                          }));

                      assertThat(results)
                          .containsExactlyInAnyOrder(
                              Grouped.of(0, List.of(0, 10, 20)),
                              Grouped.of(1, List.of(1, 11, 21)),
                              Grouped.of(2, List.of(2, 12, 22)),
                              Grouped.of(3, List.of(3, 13, 23)),
                              Grouped.of(4, List.of(4, 14, 24)),
                              Grouped.of(5, List.of(5, 15, 25)),
                              Grouped.of(6, List.of(6, 16, 26)),
                              Grouped.of(7, List.of(7, 17, 27)),
                              Grouped.of(8, List.of(8, 18, 28)),
                              Grouped.of(9, List.of(9, 19, 29)));

                      assertThat(threads)
                          .hasSizeLessThanOrEqualTo(10)
                          .allSatisfy(
                              (t, list) -> {
                                assertThat(list.size() % 3).isZero();
                                IntStream.range(0, (list.size() / 3))
                                    .mapToObj(i -> list.get(i * 3))
                                    .forEachOrdered(
                                        s -> assertThat(list).containsSequence(s, s + 10, s + 20));
                              });

                      assertThat(results.stream().flatMap(g -> g.values().stream()).toList())
                          .hasSize(30)
                          .containsExactlyInAnyOrderElementsOf(
                              IntStream.range(0, 10)
                                  .boxed()
                                  .flatMap(i -> Stream.of(i, i + 10, i + 20))
                                  .toList());
                    }));
  }

  @TestFactory
  Stream<DynamicTest> shouldGroupByClassifierOrdered() {
    return allGroupingOrdered(i -> i % 10, 100)
        .map(
            c ->
                DynamicTest.dynamicTest(
                    c.name(),
                    () -> {
                      Map<Thread, List<Integer>> threads = new ConcurrentHashMap<>();

                      List<Grouped<Integer, Integer>> results =
                          Stream.iterate(0, i -> i + 1)
                              .limit(30)
                              .collect(
                                  c.factory()
                                      .collector(
                                          i -> {
                                            threads
                                                .computeIfAbsent(
                                                    Thread.currentThread(),
                                                    (k) -> new ArrayList<>())
                                                .add(i);
                                            return i;
                                          }));

                      assertThat(results)
                          .containsExactly(
                              Grouped.of(0, List.of(0, 10, 20)),
                              Grouped.of(1, List.of(1, 11, 21)),
                              Grouped.of(2, List.of(2, 12, 22)),
                              Grouped.of(3, List.of(3, 13, 23)),
                              Grouped.of(4, List.of(4, 14, 24)),
                              Grouped.of(5, List.of(5, 15, 25)),
                              Grouped.of(6, List.of(6, 16, 26)),
                              Grouped.of(7, List.of(7, 17, 27)),
                              Grouped.of(8, List.of(8, 18, 28)),
                              Grouped.of(9, List.of(9, 19, 29)));

                      assertThat(threads)
                          .hasSizeLessThanOrEqualTo(10)
                          .allSatisfy(
                              (t, list) -> {
                                assertThat(list.size() % 3).isZero();
                                IntStream.range(0, (list.size() / 3))
                                    .mapToObj(i -> list.get(i * 3))
                                    .forEachOrdered(
                                        s -> assertThat(list).containsSequence(s, s + 10, s + 20));
                              });

                      assertThat(results.stream().flatMap(g -> g.values().stream()).toList())
                          .hasSize(30)
                          .containsExactlyInAnyOrderElementsOf(
                              IntStream.range(0, 10)
                                  .boxed()
                                  .flatMap(i -> Stream.of(i, i + 10, i + 20))
                                  .toList());
                    }));
  }
}
