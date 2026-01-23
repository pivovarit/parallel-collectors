/*
 * Copyright 2014-2026 Grzegorz Piwowarek, https://4comprehension.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pivovarit.collectors.test;

import com.pivovarit.collectors.Grouped;
import com.pivovarit.collectors.ParallelCollectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class GroupingTest {

    static <T, R> Factory.GenericCollector<GroupingCollectorFactory<T, R>> groupingCollector(String name, GroupingCollectorFactory<T, R> collector) {
        return new Factory.GenericCollector<>(name, collector);
    }

    static Stream<Factory.GenericCollector<GroupingCollectorFactory<Integer, Integer>>> allGroupingOrdered(Function<Integer, Integer> classifier, int parallelism) {
        return Stream.of(
          groupingCollector("parallelToStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered()), c -> c.toList())),
          groupingCollector("parallelToStreamBy(p)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered().parallelism(parallelism)), c -> c.toList())),
          groupingCollector("parallelToStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered().executor(e())), c -> c.toList())),
          groupingCollector("parallelToStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered().executor(e()).parallelism(parallelism)), c -> c.toList()))
        );
    }

    static Stream<Factory.GenericCollector<GroupingCollectorFactory<Integer, Integer>>> allGrouping(Function<Integer, Integer> classifier, int parallelism) {
        return Stream.of(
          groupingCollector("parallelBy()", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f), c -> c.join().toList())),
          groupingCollector("parallelBy(e)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, c -> c.executor(e())), c -> c.join().toList())),
          groupingCollector("parallelBy(p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, parallelism), c -> c.join().toList())),
          groupingCollector("parallelBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, c -> c.executor(e()).parallelism(parallelism)), c -> c.join().toList())),
          groupingCollector("parallelBy(toList())", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, toList()), c -> c.join())),
          groupingCollector("parallelBy(toList(), p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, parallelism, toList()), c -> c.join())),
          groupingCollector("parallelBy(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, c -> c.executor(e()), toList()), c -> c.join())),
          groupingCollector("parallelBy(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, c -> c.executor(e()).parallelism(parallelism), toList()), c -> c.join())),
          groupingCollector("parallelToStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f), c -> c.toList())),
          groupingCollector("parallelToStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.executor(e())), c -> c.toList())),
          groupingCollector("parallelToStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.executor(e()).parallelism(parallelism)), c -> c.toList())),
          groupingCollector("parallelToOrderedStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered()), c -> c.toList())),
          groupingCollector("parallelToOrderedStreamBy(p)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered().parallelism(parallelism)), c -> c.toList())),
          groupingCollector("parallelToOrderedStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered().executor(e())), c -> c.toList())),
          groupingCollector("parallelToOrderedStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.executor(e()).parallelism(parallelism)), c -> c.toList()))
        );
    }

    @TestFactory
    Stream<DynamicTest> shouldGroupByClassifier() {
        return allGrouping(i -> i % 10, 100)
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              Map<Thread, List<Integer>> threads = new ConcurrentHashMap<>();

              List<Grouped<Integer, Integer>> results = Stream.iterate(0, i -> i + 1).limit(30)
                .collect(c.factory().collector(i -> {
                    threads.computeIfAbsent(Thread.currentThread(), (k) -> new ArrayList<>()).add(i);
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
                  Grouped.of(9, List.of(9, 19, 29))
                );

              assertThat(threads)
                .hasSizeLessThanOrEqualTo(10)
                .allSatisfy((t, list) -> {
                    assertThat(list.size() % 3).isZero();
                    IntStream.range(0, (list.size() / 3))
                      .mapToObj(i -> list.get(i * 3))
                      .forEachOrdered(s -> assertThat(list).containsSequence(s, s + 10, s + 20));
                });

              assertThat(results.stream().flatMap(g -> g.values().stream()).toList())
                .hasSize(30)
                .containsExactlyInAnyOrderElementsOf(IntStream.range(0, 10).boxed()
                  .flatMap(i -> Stream.of(i, i + 10, i + 20))
                  .toList());
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldGroupByClassifierOrdered() {
        return allGroupingOrdered(i -> i % 10, 100)
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              Map<Thread, List<Integer>> threads = new ConcurrentHashMap<>();

              List<Grouped<Integer, Integer>> results = Stream.iterate(0, i -> i + 1).limit(30)
                .collect(c.factory().collector(i -> {
                    threads.computeIfAbsent(Thread.currentThread(), (k) -> new ArrayList<>()).add(i);
                    return i;
                }));

              assertThat(results).containsExactly(
                Grouped.of(0, List.of(0, 10, 20)),
                Grouped.of(1, List.of(1, 11, 21)),
                Grouped.of(2, List.of(2, 12, 22)),
                Grouped.of(3, List.of(3, 13, 23)),
                Grouped.of(4, List.of(4, 14, 24)),
                Grouped.of(5, List.of(5, 15, 25)),
                Grouped.of(6, List.of(6, 16, 26)),
                Grouped.of(7, List.of(7, 17, 27)),
                Grouped.of(8, List.of(8, 18, 28)),
                Grouped.of(9, List.of(9, 19, 29))
              );

              assertThat(threads)
                .hasSizeLessThanOrEqualTo(10)
                .allSatisfy((t, list) -> {
                    assertThat(list.size() % 3).isZero();
                    IntStream.range(0, (list.size() / 3))
                      .mapToObj(i -> list.get(i * 3))
                      .forEachOrdered(s -> assertThat(list).containsSequence(s, s + 10, s + 20));
                });

              assertThat(results.stream().flatMap(g -> g.values().stream()).toList())
                .hasSize(30)
                .containsExactlyInAnyOrderElementsOf(IntStream.range(0, 10).boxed()
                  .flatMap(i -> Stream.of(i, i + 10, i + 20))
                  .toList());
          }));
    }

    static Executor e() {
        return Executors.newCachedThreadPool();
    }

    @FunctionalInterface
    interface GroupingCollectorFactory<T, R> {
        Collector<T, ?, List<Grouped<T, R>>> collector(Function<T, R> f);
    }
}
