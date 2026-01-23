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
          // parallel()
          parallel(),
          parallel(p()),
          parallel(c -> {}),
          parallel(c -> c.parallelism(p()), "parallelism"),
          parallel(c -> c.executor(e()), "executor"),
          parallel(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          parallel(c -> c.executor(e()).parallelism(p()).batching(), "executor", "parallelism", "batching"),
          // parallel() with custom collector
          parallelToList(),
          parallelToList(p()),
          parallelToList(c -> c.executor(e()), "executor"),
          parallelToList(c -> c.parallelism(p()), "parallelism"),
          parallelToList(c -> c.executor(e()).parallelism(p()), "parallelism", "executor"),
          parallelToList(c -> c.parallelism(p()).batching(), "parallelism", "batching"),
          parallelToList(c -> c.batching().executor(e()).parallelism(p()), "parallelism", "executor", "batching"),
          // parallelBy()
          parallelBy(),
          parallelBy(p()),
          parallelBy(c -> {}),
          parallelBy(c -> c.parallelism(p()), "parallelism"),
          parallelBy(c -> c.executor(e()), "executor"),
          parallelBy(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          parallelBy(c -> c.executor(e()).parallelism(p()).batching(), "executor", "parallelism", "batching"),
          // parallelBy() with customer collector
          parallelByToList(),
          parallelByToList(p()),
          parallelByToList(c -> {}),
          parallelByToList(c -> c.parallelism(p()), "parallelism"),
          parallelByToList(c -> c.executor(e()), "executor"),
          parallelByToList(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          parallelByToList(c -> c.executor(e()).parallelism(p()).batching(), "executor", "parallelism", "batching"),
          // parallelToStream() unordered
          parallelToStream(),
          parallelToStream(p()),
          parallelToStream(c -> {}),
          parallelToStream(c -> c.parallelism(p()), "parallelism"),
          parallelToStream(c -> c.executor(e()), "executor"),
          parallelToStream(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          parallelToStream(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          // parallelToStream() unordered and batching
          parallelToStream(c -> c.batching().parallelism(p()), "parallelism", "batching"),
          parallelToStream(c -> c.batching().executor(e()).parallelism(p()), "executor", "parallelism", "batching"),
          // parallelToStream() ordered
          parallelToStream(c -> c.ordered(), "ordered"),
          parallelToStream(c -> c.ordered().parallelism(p()), "ordered", "parallelism"),
          parallelToStream(c -> c.ordered().executor(e()), "ordered", "executor"),
          parallelToStream(c -> c.ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism"),
          // parallelToStream() ordered and batching
          parallelToStream(c -> c.batching().ordered().parallelism(p()), "ordered", "parallelism", "batching"),
          parallelToStream(c -> c.batching().ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism", "batching"),
          // parallelToStreamBy()
          parallelToStreamBy(),
          parallelToStreamBy(p()),
          parallelToStreamBy(c -> {}),
          // parallelToStreamBy() ordered
          parallelToStreamBy(c -> c.ordered(), "ordered"),
          parallelToStreamBy(c -> c.ordered().parallelism(p()), "ordered", "parallelism"),
          parallelToStreamBy(c -> c.ordered().executor(e()), "ordered", "executor"),
          parallelToStreamBy(c -> c.ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism"),
          // parallelToStreamBy() ordered and batching
          parallelToStreamBy(c -> c.batching().ordered().parallelism(p()), "ordered", "parallelism", "batching"),
          parallelToStreamBy(c -> c.batching().ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism", "batching"),
          // parallelToStreamBy() unordered
          parallelToStreamBy(c -> {}),
          parallelToStreamBy(c -> c.parallelism(p()), "parallelism"),
          parallelToStreamBy(c -> c.executor(e()), "executor"),
          parallelToStreamBy(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          // parallelToStreamBy() unordered and batching
          parallelToStreamBy(c -> c.batching().parallelism(p()), "parallelism", "batching"),
          parallelToStreamBy(c -> c.batching().executor(e()).parallelism(p()), "executor", "parallelism", "batching")
        );
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelBy() {
        return new GenericCollector<>("parallelBy()", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, p()), c -> ungrouped(c.thenApply(Stream::toList))));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelBy(int parallelism) {
        return new GenericCollector<>("parallelBy(%s)".formatted(parallelism), f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, parallelism), c -> ungrouped(c.thenApply(Stream::toList))));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelBy(Consumer<CollectingConfigurer> configurer, String... tags) {
        String name = "parallelBy() [%s]".formatted(String.join(", ", tags));
        return new GenericCollector<>(name, f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, configurer), c -> ungrouped(c.thenApply(Stream::toList))));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallel() {
        return new GenericCollector<>("parallel()", f -> collectingAndThen(ParallelCollectors.parallel(f), c -> c.join().toList()));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallel(int parallelism) {
        return new GenericCollector<>("parallel(%s)".formatted(parallelism), f -> collectingAndThen(ParallelCollectors.parallel(f, parallelism), c -> c.join().toList()));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallel(Consumer<CollectingConfigurer> configurer, String... tags) {
        String name = "parallel() [%s]".formatted(String.join(", ", tags));
        return new GenericCollector<>(name, f -> collectingAndThen(ParallelCollectors.parallel(f, configurer), c -> c.join().toList()));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelToList() {
        return new GenericCollector<>("parallel(toList())", f -> collectingAndThen(ParallelCollectors.parallel(f, toList()), CompletableFuture::join));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelToList(int parallelism) {
        return new GenericCollector<>("parallel(%s, toList())".formatted(parallelism), f -> collectingAndThen(ParallelCollectors.parallel(f, parallelism, toList()), CompletableFuture::join));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelToList(Consumer<CollectingConfigurer> configurer, String... tags) {
        String name = "parallel(toList()) [%s]".formatted(String.join(", ", tags));
        return new GenericCollector<>(name, f -> collectingAndThen(ParallelCollectors.parallel(f, configurer, toList()), CompletableFuture::join));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelByToList() {
        return new GenericCollector<>("parallelBy(toList())", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList()), c -> ungrouped(c.join())));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelByToList(int parallelism) {
        return new GenericCollector<>("parallelBy(%s, toList())".formatted(parallelism), f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, parallelism, toList()), c -> ungrouped(c.join())));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelByToList(Consumer<CollectingConfigurer> configurer, String... tags) {
        String name = "parallelBy(toList())[%s]".formatted(String.join(", ", tags));
        return new GenericCollector<>(name, f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, configurer, toList()), c -> ungrouped(c.join())));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelToStreamBy() {
        return new GenericCollector<>("parallelToStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f), ungrouped()));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelToStreamBy(int parallelism) {
        return new GenericCollector<>("parallelToStreamBy(%s)".formatted(parallelism), f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, parallelism), ungrouped()));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelToStreamBy(Consumer<StreamingConfigurer> configurer, String... tags) {
        String name = "parallelToStreamBy() [%s]".formatted(String.join(", ", tags));
        return new GenericCollector<>(name, f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, configurer), ungrouped()));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelToStream() {
        return new GenericCollector<>("parallelToStream()", f -> collectingAndThen(ParallelCollectors.parallelToStream(f), Stream::toList));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelToStream(int parallelism) {
        return new GenericCollector<>("parallelToStream(%s)".formatted(parallelism), f -> collectingAndThen(ParallelCollectors.parallelToStream(f, parallelism), Stream::toList));
    }

    public static GenericCollector<CollectorFactory<Integer, Integer>> parallelToStream(Consumer<StreamingConfigurer> configurer, String... tags) {
        String name = "parallelToStream() [%s]".formatted(String.join(", ", tags));
        return new GenericCollector<>(name, f -> collectingAndThen(ParallelCollectors.parallelToStream(f, configurer), Stream::toList));
    }

    static Stream<Factory.GenericCollector<Factory.CollectorFactory<Integer, Integer>>> allNonGrouping() {
        return Stream.of(
          // parallel()
          parallel(),
          parallel(p()),
          parallel(c -> {}),
          parallel(c -> c.parallelism(p()), "parallelism"),
          parallel(c -> c.executor(e()), "executor"),
          parallel(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          parallel(c -> c.executor(e()).parallelism(p()).batching(), "executor", "parallelism", "batching"),
          // parallel() with custom collector
          parallelToList(),
          parallelToList(p()),
          parallelToList(c -> c.executor(e()), "executor"),
          parallelToList(c -> c.parallelism(p()), "parallelism"),
          parallelToList(c -> c.executor(e()).parallelism(p()), "parallelism", "executor"),
          parallelToList(c -> c.parallelism(p()).batching(), "parallelism", "batching"),
          parallelToList(c -> c.batching().executor(e()).parallelism(p()), "parallelism", "executor", "batching"),
          // parallelToStream() unordered
          parallelToStream(),
          parallelToStream(p()),
          parallelToStream(c -> {}),
          parallelToStream(c -> c.parallelism(p()), "parallelism"),
          parallelToStream(c -> c.executor(e()), "executor"),
          parallelToStream(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          parallelToStream(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          // parallelToStream() unordered and batching
          parallelToStream(c -> c.batching().parallelism(p()), "parallelism", "batching"),
          parallelToStream(c -> c.batching().executor(e()).parallelism(p()), "executor", "parallelism", "batching"),
          // parallelToStream() ordered
          parallelToStream(c -> c.ordered(), "ordered"),
          parallelToStream(c -> c.ordered().parallelism(p()), "ordered", "parallelism"),
          parallelToStream(c -> c.ordered().executor(e()), "ordered", "executor"),
          parallelToStream(c -> c.ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism"),
          // parallelToStream() ordered and batching
          parallelToStream(c -> c.batching().ordered().parallelism(p()), "ordered", "parallelism", "batching"),
          parallelToStream(c -> c.batching().ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism", "batching")
        );
    }

    static Stream<Factory.GenericCollector<Factory.GroupingCollectorFactory<Integer, Integer>>> allGrouping(Function<Integer, Integer> classifier, int parallelism) {
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

    static Stream<Factory.GenericCollector<Factory.GroupingCollectorFactory<Integer, Integer>>> allGroupingOrdered(Function<Integer, Integer> classifier, int parallelism) {
        return Stream.of(
          groupingCollector("parallelToStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered()), c -> c.toList())),
          groupingCollector("parallelToStreamBy(p)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered().parallelism(parallelism)), c -> c.toList())),
          groupingCollector("parallelToStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered().executor(e())), c -> c.toList())),
          groupingCollector("parallelToStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered().executor(e()).parallelism(parallelism)), c -> c.toList()))
        );
    }

    static Stream<Factory.GenericCollector<Factory.CollectorFactory<Integer, Integer>>> allOrdered() {
        return Stream.of(
          // parallel()
          parallel(),
          parallel(p()),
          parallel(c -> {}),
          parallel(c -> c.parallelism(p()), "parallelism"),
          parallel(c -> c.executor(e()), "executor"),
          parallel(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          parallel(c -> c.executor(e()).parallelism(p()).batching(), "executor", "parallelism", "batching"),
          // parallel() with custom collector
          parallelToList(),
          parallelToList(p()),
          parallelToList(c -> c.executor(e()), "executor"),
          parallelToList(c -> c.parallelism(p()), "parallelism"),
          parallelToList(c -> c.executor(e()).parallelism(p()), "parallelism", "executor"),
          parallelToList(c -> c.parallelism(p()).batching(), "parallelism", "batching"),
          parallelToList(c -> c.batching().executor(e()).parallelism(p()), "parallelism", "executor", "batching"),
          // parallelBy()
          parallelBy(),
          parallelBy(p()),
          parallelBy(c -> {}),
          parallelBy(c -> c.parallelism(p()), "parallelism"),
          parallelBy(c -> c.executor(e()), "executor"),
          parallelBy(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          parallelBy(c -> c.executor(e()).parallelism(p()).batching(), "executor", "parallelism", "batching"),
          // parallelBy() with customer collector
          parallelByToList(),
          parallelByToList(p()),
          parallelByToList(c -> {}),
          parallelByToList(c -> c.parallelism(p()), "parallelism"),
          parallelByToList(c -> c.executor(e()), "executor"),
          parallelByToList(c -> c.executor(e()).parallelism(p()), "executor", "parallelism"),
          parallelByToList(c -> c.executor(e()).parallelism(p()).batching(), "executor", "parallelism", "batching"),
          // parallelToStream() ordered
          parallelToStream(c -> c.ordered(), "ordered"),
          parallelToStream(c -> c.ordered().parallelism(p()), "ordered", "parallelism"),
          parallelToStream(c -> c.ordered().executor(e()), "ordered", "executor"),
          parallelToStream(c -> c.ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism"),
          // parallelToStream() ordered and batching
          parallelToStream(c -> c.batching().ordered().parallelism(p()), "ordered", "parallelism", "batching"),
          parallelToStream(c -> c.batching().ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism", "batching"),
          // parallelToStreamBy()
          parallelToStreamBy(),
          parallelToStreamBy(p()),
          parallelToStreamBy(c -> {}),
          // parallelToStreamBy() ordered
          parallelToStreamBy(c -> c.ordered(), "ordered"),
          parallelToStreamBy(c -> c.ordered().parallelism(p()), "ordered", "parallelism"),
          parallelToStreamBy(c -> c.ordered().executor(e()), "ordered", "executor"),
          parallelToStreamBy(c -> c.ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism"),
          // parallelToStreamBy() ordered and batching
          parallelToStreamBy(c -> c.batching().ordered().parallelism(p()), "ordered", "parallelism", "batching"),
          parallelToStreamBy(c -> c.batching().ordered().executor(e()).parallelism(p()), "ordered", "executor", "parallelism", "batching")
        );
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

    public static Stream<GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>>> boundedCollectors() {
        return Stream.of(
          advancedCollector("parallel()", (f, e, p) -> ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(p))),
          advancedCollector("parallel(toList())", (f, e, p) -> ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(p), toList())),
          advancedCollector("parallelBy()", (f, e, p) -> ParallelCollectors.parallelBy(noopClassifier(), f, c -> c.executor(e).parallelism(p))),
          advancedCollector("parallelBy(toList())", (f, e, p) -> ParallelCollectors.parallelBy(noopClassifier(), f, c -> c.executor(e).parallelism(p), toList())),
          advancedCollector("parallelToStream()", (f, e, p) -> ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(p))),
          advancedCollector("parallelToStreamBy()", (f, e, p) -> ParallelCollectors.parallelToStreamBy(noopClassifier(), f, c -> c.executor(e).parallelism(p))),
          advancedCollector("parallelToOrderedStream()", (f, e, p) -> ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(p).ordered())),
          advancedCollector("parallelToOrderedStreamBy()", (f, e, p) -> ParallelCollectors.parallelToStreamBy(noopClassifier(), f, c -> c.executor(e).parallelism(p).ordered())),
          advancedCollector("parallel() (batching)", (f, e, p) -> ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(p).batching())),
          advancedCollector("parallel(toList()) (batching)", (f, e, p) -> ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(p).batching(), toList())),
          advancedCollector("parallelToStream() (batching)", (f, e, p) -> ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(p).batching())),
          advancedCollector("parallelToOrderedStream() (batching)", (f, e, p) -> ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(p).batching().ordered())));
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
