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

import com.pivovarit.collectors.Group;
import com.pivovarit.collectors.ParallelCollectors;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.test.Factory.allNonGrouping;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParallelismValidationTest {

    static <T, R> Factory.GenericCollector<Factory.CollectorFactoryWithParallelism<T, R>> limitedCollector(String name, Factory.CollectorFactoryWithParallelism<T, R> collector) {
        return new Factory.GenericCollector<>(name, collector);
    }

    static Stream<Factory.GenericCollector<Factory.CollectorFactoryWithParallelism<Integer, Integer>>> allBounded() {
        return Stream.of(
          limitedCollector("parallel(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, p), c -> c.join().toList())),
          limitedCollector("parallel(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e()).parallelism(p)), c -> c.join().toList())),
          limitedCollector("parallel(toList(), p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, p, toList()), CompletableFuture::join)),
          limitedCollector("parallel(toList(), e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e()).parallelism(p), toList()), CompletableFuture::join)),
          limitedCollector("parallel(toList(), e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.batching().executor(e()).parallelism(p), toList()), CompletableFuture::join)),
          limitedCollector("parallelBy(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, p), c -> ungrouped(c.join().toList()))),
          limitedCollector("parallelBy(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, c -> c.executor(e()).parallelism(p)), c -> ungrouped(c.join().toList()))),
          limitedCollector("parallelBy(toList(), p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, p, toList()), c -> ungrouped(c.join()))),
          limitedCollector("parallelBy(toList(), e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, c -> c.executor(e()).parallelism(p), toList()), c -> ungrouped(c.join()))),
          limitedCollector("parallelToStream(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, p), Stream::toList)),
          limitedCollector("parallelToStream(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e()).parallelism(p)), Stream::toList)),
          limitedCollector("parallelToStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.batching().executor(e()).parallelism(p)), Stream::toList)),
          limitedCollector("parallelToStreamBy(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, p), s -> s.map(Group::values).flatMap(Collection::stream).toList())),
          limitedCollector("parallelToStreamBy(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, c -> c.executor(e()).parallelism(p)), ungrouped())),
          limitedCollector("parallelToOrderedStream(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.ordered().parallelism(p)), Stream::toList)),
          limitedCollector("parallelToOrderedStream(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.ordered().executor(e()).parallelism(p)), Stream::toList)),
          limitedCollector("parallelToOrderedStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.ordered().batching().executor(e()).parallelism(p)), Stream::toList)),
          limitedCollector("parallelToOrderedStreamBy(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, c -> c.parallelism(p).ordered()), ungrouped())),
          limitedCollector("parallelToOrderedStreamBy(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, c -> c.ordered().executor(e()).parallelism(p)), ungrouped()))
        );
    }

    @TestFactory
    Stream<DynamicTest> shouldThrowOnParallelStream() {
        return allNonGrouping()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              assertThatThrownBy(() -> Stream.generate(() -> 42)
                .limit(1000)
                .parallel()
                .collect(c.factory().collector(i -> i)))
                .isExactlyInstanceOf(UnsupportedOperationException.class);
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldRejectInvalidParallelism() {
        return allBounded()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              assertThatThrownBy(() -> {
                  List<Integer> ignored = Stream.of(1).collect(c.factory().collector(i -> i, -1));
              }).isInstanceOf(IllegalArgumentException.class);
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldRejectInvalidParallelismEagerly() {
        return allBounded()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              assertThatThrownBy(() -> c.factory().collector(i -> i, -1))
                .isInstanceOf(IllegalArgumentException.class);
          }));
    }

    static <T> Function<T, UUID> noopClassifier() {
        return i -> UUID.randomUUID();
    }

    static Executor e() {
        return Executors.newCachedThreadPool();
    }

    private static <K, V> Function<Stream<Group<K, V>>, List<V>> ungrouped() {
        return s -> s.flatMap(g -> g.values().stream()).toList();
    }

    private static <K, V> List<V> ungrouped(Collection<Group<K, V>> collection) {
        return collection.stream().flatMap(g -> g.values().stream()).toList();
    }
}
