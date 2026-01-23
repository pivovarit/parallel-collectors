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

import com.pivovarit.collectors.CollectingConfigurer;
import com.pivovarit.collectors.Grouped;
import com.pivovarit.collectors.ParallelCollectors;
import com.pivovarit.collectors.StreamingConfigurer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

final class Factory {

    private Factory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static Stream<Factory.GenericCollector<Factory.CollectorFactory<Integer, Integer>>> all() {
        return Stream.of(
          // parallel()
          CollectorFactory.parallel(),
          CollectorFactory.parallel(p()),
          CollectorFactory.parallel(c -> {}),
          CollectorFactory.parallel(c -> c.parallelism(p()), "parallelism"),
          CollectorFactory.parallel(c -> c.executor(e()), "executor"),
          CollectorFactory.parallel(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          CollectorFactory.parallel(c -> c.executor(e()).parallelism(p()).batching(), "executor", "parallelism", "batching"),
          // parallel() with custom collector
          CollectorFactory.parallelToList(),
          CollectorFactory.parallelToList(p()),
          CollectorFactory.parallelToList(c -> c.executor(e()), "executor"),
          CollectorFactory.parallelToList(c -> c.parallelism(p()), "parallelism"),
          CollectorFactory.parallelToList(c -> c.executor(e()).parallelism(p()), "parallelism", "executor"),
          CollectorFactory.parallelToList(c -> c.parallelism(p()).batching(), "parallelism", "batching"),
          CollectorFactory.parallelToList(c -> c.batching().executor(e()).parallelism(p()), "parallelism", "executor", "batching"),
          // parallelBy()
          CollectorFactory.parallelBy(),
          CollectorFactory.parallelBy(p()),
          CollectorFactory.parallelBy(c -> {}),
          CollectorFactory.parallelBy(c -> c.parallelism(p()), "parallelism"),
          CollectorFactory.parallelBy(c -> c.executor(e()), "executor"),
          CollectorFactory.parallelBy(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          CollectorFactory.parallelBy(c -> c.executor(e()).parallelism(p()).batching(), "executor", "parallelism", "batching"),
          // parallelBy() with customer collector
          CollectorFactory.parallelByToList(),
          CollectorFactory.parallelByToList(p()),
          CollectorFactory.parallelByToList(c -> {}),
          CollectorFactory.parallelByToList(c -> c.parallelism(p()), "parallelism"),
          CollectorFactory.parallelByToList(c -> c.executor(e()), "executor"),
          CollectorFactory.parallelByToList(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          CollectorFactory.parallelByToList(c -> c.executor(e()).parallelism(p()).batching(), "executor", "parallelism", "batching"),
          // parallelToStream() unordered
          CollectorFactory.parallelToStream(),
          CollectorFactory.parallelToStream(p()),
          CollectorFactory.parallelToStream(c -> {}),
          CollectorFactory.parallelToStream(c -> c.parallelism(p()), "parallelism"),
          CollectorFactory.parallelToStream(c -> c.executor(e()), "executor"),
          CollectorFactory.parallelToStream(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          CollectorFactory.parallelToStream(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          // parallelToStream() unordered and batching
          CollectorFactory.parallelToStream(c -> c.batching().parallelism(p()), "parallelism", "batching"),
          CollectorFactory.parallelToStream(c -> c.batching().executor(e()).parallelism(p()), "executor", "parallelism", "batching"),
          // parallelToStream() ordered
          CollectorFactory.parallelToStream(c -> c.ordered(), "ordered"),
          CollectorFactory.parallelToStream(c -> c.ordered().parallelism(p()), "ordered", "parallelism"),
          CollectorFactory.parallelToStream(c -> c.ordered().executor(e()), "ordered", "executor"),
          CollectorFactory.parallelToStream(c -> c.ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism"),
          // parallelToStream() ordered and batching
          CollectorFactory.parallelToStream(c -> c.batching().ordered().parallelism(p()), "ordered", "parallelism", "batching"),
          CollectorFactory.parallelToStream(c -> c.batching().ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism", "batching"),
          // parallelToStreamBy()
          CollectorFactory.parallelToStreamBy(),
          CollectorFactory.parallelToStreamBy(p()),
          CollectorFactory.parallelToStreamBy(c -> {}),
          // parallelToStreamBy() ordered
          CollectorFactory.parallelToStreamBy(c -> c.ordered(), "ordered"),
          CollectorFactory.parallelToStreamBy(c -> c.ordered().parallelism(p()), "ordered", "parallelism"),
          CollectorFactory.parallelToStreamBy(c -> c.ordered().executor(e()), "ordered", "executor"),
          CollectorFactory.parallelToStreamBy(c -> c.ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism"),
          // parallelToStreamBy() ordered and batching
          CollectorFactory.parallelToStreamBy(c -> c.batching().ordered().parallelism(p()), "ordered", "parallelism", "batching"),
          CollectorFactory.parallelToStreamBy(c -> c.batching().ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism", "batching"),
          // parallelToStreamBy() unordered
          CollectorFactory.parallelToStreamBy(c -> {}),
          CollectorFactory.parallelToStreamBy(c -> c.parallelism(p()), "parallelism"),
          CollectorFactory.parallelToStreamBy(c -> c.executor(e()), "executor"),
          CollectorFactory.parallelToStreamBy(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          // parallelToStreamBy() unordered and batching
          CollectorFactory.parallelToStreamBy(c -> c.batching().parallelism(p()), "parallelism", "batching"),
          CollectorFactory.parallelToStreamBy(c -> c.batching().executor(e()).parallelism(p()), "executor", "parallelism", "batching")
        );
    }

    static Stream<Factory.GenericCollector<Factory.CollectorFactory<Integer, Integer>>> allNonGrouping() {
        return Stream.of(
          // parallel()
          CollectorFactory.parallel(),
          CollectorFactory.parallel(p()),
          CollectorFactory.parallel(c -> {}),
          CollectorFactory.parallel(c -> c.parallelism(p()), "parallelism"),
          CollectorFactory.parallel(c -> c.executor(e()), "executor"),
          CollectorFactory.parallel(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          CollectorFactory.parallel(c -> c.executor(e()).parallelism(p()).batching(), "executor", "parallelism", "batching"),
          // parallel() with custom collector
          CollectorFactory.parallelToList(),
          CollectorFactory.parallelToList(p()),
          CollectorFactory.parallelToList(c -> c.executor(e()), "executor"),
          CollectorFactory.parallelToList(c -> c.parallelism(p()), "parallelism"),
          CollectorFactory.parallelToList(c -> c.executor(e()).parallelism(p()), "parallelism", "executor"),
          CollectorFactory.parallelToList(c -> c.parallelism(p()).batching(), "parallelism", "batching"),
          CollectorFactory.parallelToList(c -> c.batching().executor(e()).parallelism(p()), "parallelism", "executor", "batching"),
          // parallelToStream() unordered
          CollectorFactory.parallelToStream(),
          CollectorFactory.parallelToStream(p()),
          CollectorFactory.parallelToStream(c -> {}),
          CollectorFactory.parallelToStream(c -> c.parallelism(p()), "parallelism"),
          CollectorFactory.parallelToStream(c -> c.executor(e()), "executor"),
          CollectorFactory.parallelToStream(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          CollectorFactory.parallelToStream(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          // parallelToStream() unordered and batching
          CollectorFactory.parallelToStream(c -> c.batching().parallelism(p()), "parallelism", "batching"),
          CollectorFactory.parallelToStream(c -> c.batching().executor(e()).parallelism(p()), "executor", "parallelism", "batching"),
          // parallelToStream() ordered
          CollectorFactory.parallelToStream(c -> c.ordered(), "ordered"),
          CollectorFactory.parallelToStream(c -> c.ordered().parallelism(p()), "ordered", "parallelism"),
          CollectorFactory.parallelToStream(c -> c.ordered().executor(e()), "ordered", "executor"),
          CollectorFactory.parallelToStream(c -> c.ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism"),
          // parallelToStream() ordered and batching
          CollectorFactory.parallelToStream(c -> c.batching().ordered().parallelism(p()), "ordered", "parallelism", "batching"),
          CollectorFactory.parallelToStream(c -> c.batching().ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism", "batching")
        );
    }

    static Stream<Factory.GenericCollector<Factory.CollectorFactory<Integer, Integer>>> allOrdered() {
        return Stream.of(
          // parallel()
          CollectorFactory.parallel(),
          CollectorFactory.parallel(p()),
          CollectorFactory.parallel(c -> {}),
          CollectorFactory.parallel(c -> c.parallelism(p()), "parallelism"),
          CollectorFactory.parallel(c -> c.executor(e()), "executor"),
          CollectorFactory.parallel(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          CollectorFactory.parallel(c -> c.executor(e()).parallelism(p()).batching(), "executor", "parallelism", "batching"),
          // parallel() with custom collector
          CollectorFactory.parallelToList(),
          CollectorFactory.parallelToList(p()),
          CollectorFactory.parallelToList(c -> c.executor(e()), "executor"),
          CollectorFactory.parallelToList(c -> c.parallelism(p()), "parallelism"),
          CollectorFactory.parallelToList(c -> c.executor(e()).parallelism(p()), "parallelism", "executor"),
          CollectorFactory.parallelToList(c -> c.parallelism(p()).batching(), "parallelism", "batching"),
          CollectorFactory.parallelToList(c -> c.batching().executor(e()).parallelism(p()), "parallelism", "executor", "batching"),
          // parallelBy()
          CollectorFactory.parallelBy(),
          CollectorFactory.parallelBy(p()),
          CollectorFactory.parallelBy(c -> {}),
          CollectorFactory.parallelBy(c -> c.parallelism(p()), "parallelism"),
          CollectorFactory.parallelBy(c -> c.executor(e()), "executor"),
          CollectorFactory.parallelBy(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          CollectorFactory.parallelBy(c -> c.executor(e()).parallelism(p()).batching(), "executor", "parallelism", "batching"),
          // parallelBy() with customer collector
          CollectorFactory.parallelByToList(),
          CollectorFactory.parallelByToList(p()),
          CollectorFactory.parallelByToList(c -> {}),
          CollectorFactory.parallelByToList(c -> c.parallelism(p()), "parallelism"),
          CollectorFactory.parallelByToList(c -> c.executor(e()), "executor"),
          CollectorFactory.parallelByToList(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          CollectorFactory.parallelByToList(c -> c.executor(e()).parallelism(p()).batching(), "executor", "parallelism", "batching"),
          // parallelToStream() ordered
          CollectorFactory.parallelToStream(c -> c.ordered(), "ordered"),
          CollectorFactory.parallelToStream(c -> c.ordered().parallelism(p()), "ordered", "parallelism"),
          CollectorFactory.parallelToStream(c -> c.ordered().executor(e()), "ordered", "executor"),
          CollectorFactory.parallelToStream(c -> c.ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism"),
          // parallelToStream() ordered and batching
          CollectorFactory.parallelToStream(c -> c.batching().ordered().parallelism(p()), "ordered", "parallelism", "batching"),
          CollectorFactory.parallelToStream(c -> c.batching().ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism", "batching"),
          // parallelToStreamBy()
          CollectorFactory.parallelToStreamBy(),
          CollectorFactory.parallelToStreamBy(p()),
          CollectorFactory.parallelToStreamBy(c -> {}),
          // parallelToStreamBy() ordered
          CollectorFactory.parallelToStreamBy(c -> c.ordered(), "ordered"),
          CollectorFactory.parallelToStreamBy(c -> c.ordered().parallelism(p()), "ordered", "parallelism"),
          CollectorFactory.parallelToStreamBy(c -> c.ordered().executor(e()), "ordered", "executor"),
          CollectorFactory.parallelToStreamBy(c -> c.ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism"),
          // parallelToStreamBy() ordered and batching
          CollectorFactory.parallelToStreamBy(c -> c.batching().ordered().parallelism(p()), "ordered", "parallelism", "batching"),
          CollectorFactory.parallelToStreamBy(c -> c.batching().ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism", "batching")
        );
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

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelBy() {
            return new GenericCollector<>("parallelBy()", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, p()), c -> ungrouped(c.thenApply(Stream::toList))));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelBy(int parallelism) {
            return new GenericCollector<>("parallelBy(%s)".formatted(parallelism), f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, parallelism), c -> ungrouped(c.thenApply(Stream::toList))));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelBy(Consumer<CollectingConfigurer> configurer, String... tags) {
            String name = "parallelBy() [%s]".formatted(String.join(", ", tags));
            return new GenericCollector<>(name, f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, configurer), c -> ungrouped(c.thenApply(Stream::toList))));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallel() {
            return new GenericCollector<>("parallel()", f -> collectingAndThen(ParallelCollectors.parallel(f), c -> c.join().toList()));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallel(int parallelism) {
            return new GenericCollector<>("parallel(%s)".formatted(parallelism), f -> collectingAndThen(ParallelCollectors.parallel(f, parallelism), c -> c.join().toList()));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallel(Consumer<CollectingConfigurer> configurer, String... tags) {
            return new GenericCollector<>("parallel() [%s]".formatted(String.join(", ", tags)), f -> collectingAndThen(ParallelCollectors.parallel(f, configurer), c -> c.join().toList()));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelToList() {
            return new GenericCollector<>("parallel(toList())", f -> collectingAndThen(ParallelCollectors.parallel(f, toList()), CompletableFuture::join));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelToList(int parallelism) {
            return new GenericCollector<>("parallel(%s, toList())".formatted(parallelism), f -> collectingAndThen(ParallelCollectors.parallel(f, parallelism, toList()), CompletableFuture::join));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelToList(Consumer<CollectingConfigurer> configurer, String... tags) {
            return new GenericCollector<>("parallel(toList()) [%s]".formatted(String.join(", ", tags)), f -> collectingAndThen(ParallelCollectors.parallel(f, configurer, toList()), CompletableFuture::join));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelByToList() {
            return new GenericCollector<>("parallelBy(toList())", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList()), c -> ungrouped(c.join())));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelByToList(int parallelism) {
            return new GenericCollector<>("parallelBy(%s, toList())".formatted(parallelism), f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, parallelism, toList()), c -> ungrouped(c.join())));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelByToList(Consumer<CollectingConfigurer> configurer, String... tags) {
            return new GenericCollector<>("parallelBy(toList())[%s]".formatted(String.join(", ", tags)), f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, configurer, toList()), c -> ungrouped(c.join())));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelToStreamBy() {
            return new GenericCollector<>("parallelToStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f), ungrouped()));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelToStreamBy(int parallelism) {
            return new GenericCollector<>("parallelToStreamBy(%s)".formatted(parallelism), f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, parallelism), ungrouped()));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelToStreamBy(Consumer<StreamingConfigurer> configurer, String... tags) {
            return new GenericCollector<>("parallelToStreamBy() [%s]".formatted(String.join(", ", tags)), f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, configurer), ungrouped()));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelToStream() {
            return new GenericCollector<>("parallelToStream()", f -> collectingAndThen(ParallelCollectors.parallelToStream(f), Stream::toList));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelToStream(int parallelism) {
            return new GenericCollector<>("parallelToStream(%s)".formatted(parallelism), f -> collectingAndThen(ParallelCollectors.parallelToStream(f, parallelism), Stream::toList));
        }

        static GenericCollector<CollectorFactory<Integer, Integer>> parallelToStream(Consumer<StreamingConfigurer> configurer, String... tags) {
            String name = "parallelToStream() [%s]".formatted(String.join(", ", tags));
            return new GenericCollector<>(name, f -> collectingAndThen(ParallelCollectors.parallelToStream(f, configurer), Stream::toList));
        }
    }

    @FunctionalInterface
    interface AsyncCollectorFactory<T, R> {
        Collector<T, ?, CompletableFuture<List<R>>> collector(Function<T, R> f);
    }

    record GenericCollector<T>(String name, T factory) {
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
