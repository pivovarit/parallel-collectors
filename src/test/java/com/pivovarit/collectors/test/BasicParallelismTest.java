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
import com.pivovarit.collectors.TestUtils;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class BasicParallelismTest {

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
          limitedCollector("parallelToStreamBy(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, p), s -> s.map(Grouped::values).flatMap(Collection::stream).toList())),
          limitedCollector("parallelToStreamBy(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, c -> c.executor(e()).parallelism(p)), ungrouped())),
          limitedCollector("parallelToOrderedStream(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.ordered().parallelism(p)), Stream::toList)),
          limitedCollector("parallelToOrderedStream(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.ordered().executor(e()).parallelism(p)), Stream::toList)),
          limitedCollector("parallelToOrderedStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.ordered().batching().executor(e()).parallelism(p)), Stream::toList)),
          limitedCollector("parallelToOrderedStreamBy(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, c -> c.parallelism(p).ordered()), ungrouped())),
          limitedCollector("parallelToOrderedStreamBy(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, c -> c.ordered().executor(e()).parallelism(p)), ungrouped()))
        );
    }

    @TestFactory
    Stream<DynamicTest> shouldProcessEmptyWithMaxParallelism() {
        return Stream.of(1, 2, 4, 8, 16, 32, 64, 100)
          .flatMap(p -> allBounded()
            .map(c -> DynamicTest.dynamicTest("%s (parallelism: %d)".formatted(c.name(), p), () -> {
                assertThat(Stream.<Integer>empty().collect(c.factory().collector(i -> i, p))).isEmpty();
            })));
    }

    @TestFactory
    Stream<DynamicTest> shouldProcessAllElementsWithMaxParallelism() {
        return Stream.of(1, 2, 4, 8, 16, 32, 64, 100)
          .flatMap(p -> allBounded()
            .map(c -> DynamicTest.dynamicTest("%s (parallelism: %d)".formatted(c.name(), p), () -> {
                var list = IntStream.range(0, 100).boxed().toList();
                List<Integer> result = list.stream().collect(c.factory().collector(i -> i, p));
                assertThat(result).containsExactlyInAnyOrderElementsOf(list);
            })));
    }

    @TestFactory
    Stream<DynamicTest> shouldRespectMaxParallelism() {
        return allBounded()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var counter = new AtomicInteger(0);
              var parallelism = 4;

              assertThatCode(() -> {
                  IntStream.range(0, 100).boxed()
                    .collect(c.factory().collector(i -> {
                        int value = counter.incrementAndGet();
                        if (value > parallelism) {
                            throw new IllegalStateException("more than two tasks executing at once!");
                        }
                        Integer result = TestUtils.returnWithDelay(i, Duration.ofMillis(10));
                        counter.decrementAndGet();
                        return result;
                    }, parallelism))
                    .forEach(i -> {});
              }).doesNotThrowAnyException();
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldRejectInvalidParallelism() {
        return allBounded()
          .flatMap(c -> Stream.of(-1, 0)
            .map(p -> DynamicTest.dynamicTest("%s [p=%d]".formatted(c.name(), p), () -> {
                assertThatThrownBy(() -> Stream.of(1).collect(c.factory().collector(i -> i, p)))
                  .isExactlyInstanceOf(IllegalArgumentException.class);
            })));
    }

    static <T> Function<T, UUID> noopClassifier() {
        return i -> UUID.randomUUID();
    }

    static Executor e() {
        return Executors.newCachedThreadPool();
    }

    private static <K, V> Function<Stream<Grouped<K, V>>, List<V>> ungrouped() {
        return s -> s.flatMap(g -> g.values().stream()).toList();
    }

    private static <K, V> List<V> ungrouped(Collection<Grouped<K, V>> collection) {
        return collection.stream().flatMap(g -> g.values().stream()).toList();
    }
}
