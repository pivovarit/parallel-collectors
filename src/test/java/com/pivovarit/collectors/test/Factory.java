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
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.Batching.parallel;
import static com.pivovarit.collectors.test.Factory.GenericCollector.advancedCollector;
import static com.pivovarit.collectors.test.Factory.GenericCollector.collector;
import static com.pivovarit.collectors.test.Factory.GenericCollector.groupingCollector;
import static com.pivovarit.collectors.test.Factory.GenericCollector.limitedCollector;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

final class Factory {

    private Factory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static Stream<Factory.GenericCollector<Factory.CollectorFactory<Integer, Integer>>> all() {
        return Stream.of(
          collector("parallel()", f -> collectingAndThen(ParallelCollectors.parallel(f), c -> c.join().toList())),
          collector("parallel(p)", f -> collectingAndThen(ParallelCollectors.parallel(f, p()), c -> c.join().toList())),
          collector("parallel(e)", f -> collectingAndThen(ParallelCollectors.parallel(f, e()), c -> c.join().toList())),
          collector("parallel(e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, e(), p()), c -> c.join().toList())),
          collector("parallel(toList())", f -> collectingAndThen(ParallelCollectors.parallel(f, toList()), CompletableFuture::join)),
          collector("parallel(toList(), p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e()), CompletableFuture::join)),
          collector("parallel(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallelBy()", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f), c -> ungrouped(c.thenApply(Stream::toList)))),
          collector("parallelBy(p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, p()), c -> ungrouped(c.thenApply(Stream::toList)))),
          collector("parallelBy(e)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, e()), c -> ungrouped(c.thenApply(Stream::toList)))),
          collector("parallelBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, e(), p()), c -> ungrouped(c.thenApply(Stream::toList)))),
          collector("parallelBy(toList())", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList()), c -> ungrouped(c.join()))),
          collector("parallelBy(toList(), p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), p()), c -> ungrouped(c.join()))),
          collector("parallelBy(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), e()), c -> ungrouped(c.join()))),
          collector("parallelBy(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), e(), p()), c -> ungrouped(c.join()))),
          collector("parallelToStream()", f -> collectingAndThen(ParallelCollectors.parallelToStream(f), Stream::toList)),
          collector("parallelToStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, p()), Stream::toList)),
          collector("parallelToStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, e()), Stream::toList)),
          collector("parallelToStream(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, e(), p()), Stream::toList)),
          collector("parallelToStream(e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e(), p()), Stream::toList)),
          collector("parallelToStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f), ungrouped())),
          collector("parallelToStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, p()), ungrouped())),
          collector("parallelToStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, e()), ungrouped())),
          collector("parallelToStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, e(), p()), ungrouped())),
          collector("parallelToOrderedStream()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f), Stream::toList)),
          collector("parallelToOrderedStream(p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, p()), Stream::toList)),
          collector("parallelToOrderedStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e()), Stream::toList)),
          collector("parallelToOrderedStream(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStream(e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f), ungrouped())),
          collector("parallelToOrderedStreamBy(p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, p()), ungrouped())),
          collector("parallelToOrderedStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, e()), ungrouped())),
          collector("parallelToOrderedStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, e(), p()), ungrouped()))
        );
    }

    static <T> Stream<Factory.GenericCollector<Factory.GroupingCollectorFactory<Integer, Integer>>> allGrouping(Function<Integer, Integer> classifier, int parallelism) {
        return Stream.of(
          groupingCollector("parallelBy()", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f), c -> c.join().toList())),
          groupingCollector("parallelBy(e)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, e()), c -> c.join().toList())),
          groupingCollector("parallelBy(p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, parallelism), c -> c.join().toList())),
          groupingCollector("parallelBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, e(), parallelism), c -> c.join().toList())),
          groupingCollector("parallelBy(toList())", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, toList()), c -> c.join())),
          groupingCollector("parallelBy(toList(), p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, toList(), parallelism), c -> c.join())),
          groupingCollector("parallelBy(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, toList(), e()), c -> c.join())),
          groupingCollector("parallelBy(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, toList(), e(), parallelism), c -> c.join())),
          groupingCollector("parallelToStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f), c -> c.toList())),
          groupingCollector("parallelToStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, e()), c -> c.toList())),
          groupingCollector("parallelToStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, e(), parallelism), c -> c.toList())),
          groupingCollector("parallelToOrderedStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(classifier, f), c -> c.toList())),
          groupingCollector("parallelToOrderedStreamBy(p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(classifier, f, parallelism), c -> c.toList())),
          groupingCollector("parallelToOrderedStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(classifier, f, e()), c -> c.toList())),
          groupingCollector("parallelToOrderedStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(classifier, f, e(), parallelism), c -> c.toList()))
        );
    }

    static <T> Stream<Factory.GenericCollector<Factory.GroupingCollectorFactory<Integer, Integer>>> allGroupingOrdered(Function<Integer, Integer> classifier, int parallelism) {
        return Stream.of(
          groupingCollector("parallelToOrderedStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(classifier, f), c -> c.toList())),
          groupingCollector("parallelToOrderedStreamBy(p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(classifier, f, parallelism), c -> c.toList())),
          groupingCollector("parallelToOrderedStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(classifier, f, e()), c -> c.toList())),
          groupingCollector("parallelToOrderedStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(classifier, f, e(), parallelism), c -> c.toList()))
        );
    }

    static Stream<Factory.GenericCollector<Factory.CollectorFactory<Integer, Integer>>> allOrdered() {
        return Stream.of(
          collector("parallel()", f -> collectingAndThen(ParallelCollectors.parallel(f), c -> c.join().toList())),
          collector("parallel(p)", f -> collectingAndThen(ParallelCollectors.parallel(f, p()), c -> c.join().toList())),
          collector("parallel(e)", f -> collectingAndThen(ParallelCollectors.parallel(f, e()), c -> c.join().toList())),
          collector("parallel(e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, e(), p()), c -> c.join().toList())),
          collector("parallelBy()", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f), c -> ungrouped(c.join().toList()))),
          collector("parallelBy(p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, p()), c -> ungrouped(c.join().toList()))),
          collector("parallelBy(e)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, e()), c -> ungrouped(c.join().toList()))),
          collector("parallelBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, e(), p()), c -> ungrouped(c.join().toList()))),
          collector("parallel(toList())", f -> collectingAndThen(ParallelCollectors.parallel(f, toList()), CompletableFuture::join)),
          collector("parallel(toList(), p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e()), CompletableFuture::join)),
          collector("parallel(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallelBy(toList())", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList()), c -> ungrouped(c.join()))),
          collector("parallelBy(toList(), p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), p()), c -> ungrouped(c.join()))),
          collector("parallelBy(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), e()), c -> ungrouped(c.join()))),
          collector("parallelBy(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), e(), p()), c -> ungrouped(c.join()))),
          collector("parallelToOrderedStream()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f), Stream::toList)),
          collector("parallelToOrderedStream(p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, p()), Stream::toList)),
          collector("parallelToOrderedStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e()), Stream::toList)),
          collector("parallelToOrderedStream(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStream(e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f), ungrouped())),
          collector("parallelToOrderedStreamBy(p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, p()), ungrouped())),
          collector("parallelToOrderedStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, e()), ungrouped())),
          collector("parallelToOrderedStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, e(), p()), ungrouped()))
        );
    }

    static Stream<Factory.GenericCollector<Factory.CollectorFactoryWithParallelism<Integer, Integer>>> allBounded() {
        return Stream.of(
          limitedCollector("parallel(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, p), c -> c.join().toList())),
          limitedCollector("parallel(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, e(), p), c -> c.join().toList())),
          limitedCollector("parallel(toList(), p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), p), CompletableFuture::join)),
          limitedCollector("parallel(toList(), e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e(), p), CompletableFuture::join)),
          limitedCollector("parallel(toList(), e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e(), p), CompletableFuture::join)),
          limitedCollector("parallelBy(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, p), c -> ungrouped(c.join().toList()))),
          limitedCollector("parallelBy(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, e(), p), c -> ungrouped(c.join().toList()))),
          limitedCollector("parallelBy(toList(), p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), p), c -> ungrouped(c.join()))),
          limitedCollector("parallelBy(toList(), e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), e(), p), c -> ungrouped(c.join()))),
          limitedCollector("parallelToStream(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, p), Stream::toList)),
          limitedCollector("parallelToStream(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, e(), p), Stream::toList)),
          limitedCollector("parallelToStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e(), p), Stream::toList)),
          limitedCollector("parallelToStreamBy(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, p), s -> s.map(Grouped::values).flatMap(Collection::stream).toList())),
          limitedCollector("parallelToStreamBy(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, e(), p), ungrouped())),
          limitedCollector("parallelToOrderedStream(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, p), Stream::toList)),
          limitedCollector("parallelToOrderedStream(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e(), p), Stream::toList)),
          limitedCollector("parallelToOrderedStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p), Stream::toList)),
          limitedCollector("parallelToOrderedStreamBy(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, p), ungrouped())),
          limitedCollector("parallelToOrderedStreamBy(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, e(), p), ungrouped()))
        );
    }

    public static Stream<GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>>> boundedCollectors() {
        return Stream.of(
          advancedCollector("parallel()", ParallelCollectors::parallel),
          advancedCollector("parallel(toList())", (f, e, p) -> ParallelCollectors.parallel(f, toList(), e, p)),
          advancedCollector("parallelBy()", (f, e, p) -> ParallelCollectors.parallelBy(noopClassifier(), f, e, p)),
          advancedCollector("parallelBy(toList())", (f, e, p) -> ParallelCollectors.parallelBy(noopClassifier(), f, toList(), e, p)),
          advancedCollector("parallelToStream()", ParallelCollectors::parallelToStream),
          advancedCollector("parallelToStreamBy()", (f, e, p) -> ParallelCollectors.parallelToStreamBy(noopClassifier(), f, e, p)),
          advancedCollector("parallelToOrderedStream()", ParallelCollectors::parallelToOrderedStream),
          advancedCollector("parallelToOrderedStreamBy()", (f, e, p) -> ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, e, p)),
          advancedCollector("parallel() (batching)", ParallelCollectors.Batching::parallel),
          advancedCollector("parallel(toList()) (batching)", (f, e, p) -> parallel(f, toList(), e, p)),
          advancedCollector("parallelToStream() (batching)", ParallelCollectors.Batching::parallelToStream),
          advancedCollector("parallelToOrderedStream() (batching)", ParallelCollectors.Batching::parallelToOrderedStream));
    }

    @FunctionalInterface
    interface CollectorFactoryWithParallelismAndExecutor<T, R> {

        Collector<T, ?, ?> apply(Function<T, R> function, Executor executorService, int parallelism);
    }
    @FunctionalInterface
    interface CollectorFactoryWithExecutor<T, R> {

        Collector<T, ?, List<R>> collector(Function<T, R> f, Executor executor);
    }
    @FunctionalInterface
    interface CollectorFactoryWithParallelism<T, R> {

        Collector<T, ?, List<R>> collector(Function<T, R> f, Integer p);
    }
    @FunctionalInterface
    interface CollectorFactory<T, R> {
        Collector<T, ?, List<R>> collector(Function<T, R> f);
    }

    @FunctionalInterface
    interface GroupingCollectorFactory<T, R> {
        Collector<T, ?, List<Grouped<T, R>>> collector(Function<T, R> f);
    }

    @FunctionalInterface
    interface StreamingCollectorFactory<T, R> {

        Collector<T, ?, Stream<R>> collector(Function<T, R> f);
    }
    @FunctionalInterface
    interface AsyncCollectorFactory<T, R> {
        Collector<T, ?, CompletableFuture<List<R>>> collector(Function<T, R> f);
    }

    record GenericCollector<T>(String name, T factory) {

        static <T, R> GenericCollector<Factory.CollectorFactory<T, R>> collector(String name, Factory.CollectorFactory<T, R> collector) {
            return new GenericCollector<>(name, collector);
        }

        static <T, R> GenericCollector<Factory.GroupingCollectorFactory<T, R>> groupingCollector(String name, Factory.GroupingCollectorFactory<T, R> collector) {
            return new GenericCollector<>(name, collector);
        }

        static <T, R> GenericCollector<Factory.AsyncCollectorFactory<T, R>> asyncCollector(String name, Factory.AsyncCollectorFactory<T, R> collector) {
            return new GenericCollector<>(name, collector);
        }

        static <T, R> GenericCollector<Factory.StreamingCollectorFactory<T, R>> streamingCollector(String name, Factory.StreamingCollectorFactory<T, R> collector) {
            return new GenericCollector<>(name, collector);
        }

        static <T, R> GenericCollector<CollectorFactoryWithParallelism<T, R>> limitedCollector(String name, CollectorFactoryWithParallelism<T, R> collector) {
            return new GenericCollector<>(name, collector);
        }

        static <T, R> GenericCollector<CollectorFactoryWithExecutor<T, R>> executorCollector(String name, CollectorFactoryWithExecutor<T, R> collector) {
            return new GenericCollector<>(name, collector);
        }

        static <T, R> GenericCollector<CollectorFactoryWithParallelismAndExecutor<T, R>> advancedCollector(String name, CollectorFactoryWithParallelismAndExecutor<T, R> collector) {
            return new GenericCollector<>(name, collector);
        }

    }
    static Executor e() {
        return Executors.newCachedThreadPool();
    }

    static int p() {
        return 4;
    }

    static <T> Function<T, UUID> noopClassifier() {
        return i -> UUID.randomUUID();
    }

    private static <K, V> Function<Stream<Grouped<K, V>>, List<V>> ungrouped() {
        return s -> s.flatMap(g -> g.values().stream()).toList();
    }

    private static <K, V> List<V> ungrouped(Collection<Grouped<K, V>> collection) {
        return collection.stream().flatMap(g -> g.values().stream()).toList();
    }

    private static <K, V> List<V> ungrouped(CompletableFuture<Collection<Grouped<K, V>>> stream) {
        return stream.join().stream().flatMap(g -> g.values().stream()).toList();
    }
}
