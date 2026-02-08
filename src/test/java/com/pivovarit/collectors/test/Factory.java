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
import com.pivovarit.collectors.Group;
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

    static Stream<GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>>> allBounded() {
        return Stream.of(
          CollectorFactoryWithParallelism.parallel(),
          CollectorFactoryWithParallelism.parallelWithExecutor(),
          CollectorFactoryWithParallelism.parallelToList(),
          CollectorFactoryWithParallelism.parallelToListWithExecutor(),
          CollectorFactoryWithParallelism.parallelToListBatching(),
          CollectorFactoryWithParallelism.parallelBy(),
          CollectorFactoryWithParallelism.parallelByWithExecutor(),
          CollectorFactoryWithParallelism.parallelByToList(),
          CollectorFactoryWithParallelism.parallelByToListWithExecutor(),
          CollectorFactoryWithParallelism.parallelToStream(),
          CollectorFactoryWithParallelism.parallelToStreamWithExecutor(),
          CollectorFactoryWithParallelism.parallelToStreamBatching(),
          CollectorFactoryWithParallelism.parallelToStreamBy(),
          CollectorFactoryWithParallelism.parallelToStreamByWithExecutor(),
          CollectorFactoryWithParallelism.parallelToOrderedStream(),
          CollectorFactoryWithParallelism.parallelToOrderedStreamWithExecutor(),
          CollectorFactoryWithParallelism.parallelToOrderedStreamBatching(),
          CollectorFactoryWithParallelism.parallelToOrderedStreamBy(),
          CollectorFactoryWithParallelism.parallelToOrderedStreamByWithExecutor()
        );
    }

    static Stream<GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>>> allBatching() {
        return Stream.of(
          CollectorFactoryWithParallelism.parallelToListBatching(),
          CollectorFactoryWithParallelism.parallelToStreamBatching(),
          CollectorFactoryWithParallelism.parallelToOrderedStreamBatching()
        );
    }

    static Stream<GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>>> boundedCollectors() {
        return Stream.of(
          CollectorFactoryWithParallelismAndExecutor.parallel(),
          CollectorFactoryWithParallelismAndExecutor.parallelToList(),
          CollectorFactoryWithParallelismAndExecutor.parallelBy(),
          CollectorFactoryWithParallelismAndExecutor.parallelByToList(),
          CollectorFactoryWithParallelismAndExecutor.parallelToStream(),
          CollectorFactoryWithParallelismAndExecutor.parallelToStreamBy(),
          CollectorFactoryWithParallelismAndExecutor.parallelToOrderedStream(),
          CollectorFactoryWithParallelismAndExecutor.parallelToOrderedStreamBy(),
          CollectorFactoryWithParallelismAndExecutor.parallelBatching(),
          CollectorFactoryWithParallelismAndExecutor.parallelToListBatching(),
          CollectorFactoryWithParallelismAndExecutor.parallelToStreamBatching(),
          CollectorFactoryWithParallelismAndExecutor.parallelToOrderedStreamBatching()
        );
    }

    static Stream<GenericCollector<CollectorFactoryWithExecutor<Integer, Integer>>> allWithCustomExecutors() {
        return Stream.of(
          CollectorFactoryWithExecutor.parallel(),
          CollectorFactoryWithExecutor.parallel(4),
          CollectorFactoryWithExecutor.parallelBatching(4),
          CollectorFactoryWithExecutor.parallelToList(),
          CollectorFactoryWithExecutor.parallelToList(4),
          CollectorFactoryWithExecutor.parallelToListBatching(4),
          CollectorFactoryWithExecutor.parallelToStream(),
          CollectorFactoryWithExecutor.parallelToStream(4),
          CollectorFactoryWithExecutor.parallelToStreamBatching(4),
          CollectorFactoryWithExecutor.parallelToOrderedStream(4),
          CollectorFactoryWithExecutor.parallelToOrderedStreamBatching(4)
        );
    }

    static Stream<GenericCollector<CollectorFactoryWithExecutor<Integer, Integer>>> allWithCustomExecutorsParallelismOne() {
        return Stream.of(
          CollectorFactoryWithExecutor.parallel(1),
          CollectorFactoryWithExecutor.parallelBatching(1),
          CollectorFactoryWithExecutor.parallelToList(1),
          CollectorFactoryWithExecutor.parallelToListBatching(1)
        );
    }

    static Stream<GenericCollector<GroupingCollectorFactory<Integer, Integer>>> allGrouping(Function<Integer, Integer> classifier, int parallelism) {
        return Stream.of(
          GroupingCollectorFactory.parallelBy(classifier),
          GroupingCollectorFactory.parallelByWithExecutor(classifier),
          GroupingCollectorFactory.parallelBy(classifier, parallelism),
          GroupingCollectorFactory.parallelByWithExecutor(classifier, parallelism),
          GroupingCollectorFactory.parallelByToList(classifier),
          GroupingCollectorFactory.parallelByToList(classifier, parallelism),
          GroupingCollectorFactory.parallelByToListWithExecutor(classifier),
          GroupingCollectorFactory.parallelByToListWithExecutor(classifier, parallelism),
          GroupingCollectorFactory.parallelToStreamBy(classifier),
          GroupingCollectorFactory.parallelToStreamByWithExecutor(classifier),
          GroupingCollectorFactory.parallelToStreamByWithExecutor(classifier, parallelism),
          GroupingCollectorFactory.parallelToOrderedStreamBy(classifier),
          GroupingCollectorFactory.parallelToOrderedStreamBy(classifier, parallelism),
          GroupingCollectorFactory.parallelToOrderedStreamByWithExecutor(classifier),
          GroupingCollectorFactory.parallelToOrderedStreamByWithExecutor(classifier, parallelism)
        );
    }

    static Stream<GenericCollector<GroupingCollectorFactory<Integer, Integer>>> allGroupingOrdered(Function<Integer, Integer> classifier, int parallelism) {
        return Stream.of(
          GroupingCollectorFactory.parallelToOrderedStreamBy(classifier),
          GroupingCollectorFactory.parallelToOrderedStreamBy(classifier, parallelism),
          GroupingCollectorFactory.parallelToOrderedStreamByWithExecutor(classifier),
          GroupingCollectorFactory.parallelToOrderedStreamByWithExecutor(classifier, parallelism)
        );
    }

    static Stream<GenericCollector<AsyncCollectorFactory<Integer, Integer>>> allAsync() {
        return Stream.of(
          AsyncCollectorFactory.parallel(),
          AsyncCollectorFactory.parallelToList(),
          AsyncCollectorFactory.parallelToList(c -> c.executor(e()), "executor"),
          AsyncCollectorFactory.parallelToList(c -> c.executor(e()).parallelism(1), "executor", "p=1"),
          AsyncCollectorFactory.parallelToList(c -> c.executor(e()).parallelism(2), "executor", "p=2"),
          AsyncCollectorFactory.parallelToList(c -> c.batching().executor(e()).parallelism(1), "batching", "executor", "p=1"),
          AsyncCollectorFactory.parallelToList(c -> c.batching().executor(e()).parallelism(2), "batching", "executor", "p=2")
        );
    }

    @FunctionalInterface
    interface CollectorFactoryWithParallelismAndExecutor<T, R> {
        Collector<T, ?, ?> apply(Function<T, R> function, Executor executorService, int parallelism);

        static GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>> parallel() {
            return new GenericCollector<>("parallel()", (f, e, p) -> ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(p)));
        }

        static GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>> parallelToList() {
            return new GenericCollector<>("parallel(toList())", (f, e, p) -> ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(p), toList()));
        }

        static GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>> parallelBy() {
            return new GenericCollector<>("parallelBy()", (f, e, p) -> ParallelCollectors.parallelBy(noopClassifier(), f, c -> c.executor(e).parallelism(p)));
        }

        static GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>> parallelByToList() {
            return new GenericCollector<>("parallelBy(toList())", (f, e, p) -> ParallelCollectors.parallelBy(noopClassifier(), f, c -> c.executor(e).parallelism(p), toList()));
        }

        static GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>> parallelToStream() {
            return new GenericCollector<>("parallelToStream()", (f, e, p) -> ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(p)));
        }

        static GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>> parallelToStreamBy() {
            return new GenericCollector<>("parallelToStreamBy()", (f, e, p) -> ParallelCollectors.parallelToStreamBy(noopClassifier(), f, c -> c.executor(e).parallelism(p)));
        }

        static GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>> parallelToOrderedStream() {
            return new GenericCollector<>("parallelToOrderedStream()", (f, e, p) -> ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(p).ordered()));
        }

        static GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>> parallelToOrderedStreamBy() {
            return new GenericCollector<>("parallelToOrderedStreamBy()", (f, e, p) -> ParallelCollectors.parallelToStreamBy(noopClassifier(), f, c -> c.executor(e).parallelism(p).ordered()));
        }

        static GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>> parallelBatching() {
            return new GenericCollector<>("parallel() [batching]", (f, e, p) -> ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(p).batching()));
        }

        static GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>> parallelToListBatching() {
            return new GenericCollector<>("parallel(toList()) [batching]", (f, e, p) -> ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(p).batching(), toList()));
        }

        static GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>> parallelToStreamBatching() {
            return new GenericCollector<>("parallelToStream() [batching]", (f, e, p) -> ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(p).batching()));
        }

        static GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>> parallelToOrderedStreamBatching() {
            return new GenericCollector<>("parallelToOrderedStream() [batching]", (f, e, p) -> ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(p).batching().ordered()));
        }
    }

    @FunctionalInterface
    interface CollectorFactoryWithExecutor<T, R> {

        Collector<T, ?, List<R>> collector(Function<T, R> f, Executor executor);

        static GenericCollector<CollectorFactoryWithExecutor<Integer, Integer>> parallel() {
            return new GenericCollector<>("parallel(e)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e)), c -> c.thenApply(Stream::toList).join()));
        }

        static GenericCollector<CollectorFactoryWithExecutor<Integer, Integer>> parallel(int parallelism) {
            return new GenericCollector<>("parallel(e, p=%d)".formatted(parallelism), (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(parallelism)), c -> c.thenApply(Stream::toList).join()));
        }

        static GenericCollector<CollectorFactoryWithExecutor<Integer, Integer>> parallelBatching(int parallelism) {
            return new GenericCollector<>("parallel(e, p=%d) [batching]".formatted(parallelism), (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(parallelism).batching()), c -> c.thenApply(Stream::toList).join()));
        }

        static GenericCollector<CollectorFactoryWithExecutor<Integer, Integer>> parallelToList() {
            return new GenericCollector<>("parallel(toList(), e)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e), toList()), CompletableFuture::join));
        }

        static GenericCollector<CollectorFactoryWithExecutor<Integer, Integer>> parallelToList(int parallelism) {
            return new GenericCollector<>("parallel(toList(), e, p=%d)".formatted(parallelism), (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(parallelism), toList()), CompletableFuture::join));
        }

        static GenericCollector<CollectorFactoryWithExecutor<Integer, Integer>> parallelToListBatching(int parallelism) {
            return new GenericCollector<>("parallel(toList(), e, p=%d) [batching]".formatted(parallelism), (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(parallelism).batching(), toList()), CompletableFuture::join));
        }

        static GenericCollector<CollectorFactoryWithExecutor<Integer, Integer>> parallelToStream() {
            return new GenericCollector<>("parallelToStream(e)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e)), Stream::toList));
        }

        static GenericCollector<CollectorFactoryWithExecutor<Integer, Integer>> parallelToStream(int parallelism) {
            return new GenericCollector<>("parallelToStream(e, p=%d)".formatted(parallelism), (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(parallelism)), Stream::toList));
        }

        static GenericCollector<CollectorFactoryWithExecutor<Integer, Integer>> parallelToStreamBatching(int parallelism) {
            return new GenericCollector<>("parallelToStream(e, p=%d) [batching]".formatted(parallelism), (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(parallelism).batching()), Stream::toList));
        }

        static GenericCollector<CollectorFactoryWithExecutor<Integer, Integer>> parallelToOrderedStream(int parallelism) {
            return new GenericCollector<>("parallelToOrderedStream(e, p=%d)".formatted(parallelism), (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(parallelism).ordered()), Stream::toList));
        }

        static GenericCollector<CollectorFactoryWithExecutor<Integer, Integer>> parallelToOrderedStreamBatching(int parallelism) {
            return new GenericCollector<>("parallelToOrderedStream(e, p=%d) [batching]".formatted(parallelism), (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(parallelism).batching().ordered()), Stream::toList));
        }
    }

    @FunctionalInterface
    interface CollectorFactoryWithParallelism<T, R> {
        Collector<T, ?, List<R>> collector(Function<T, R> f, Integer p);

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallel() {
            return new GenericCollector<>("parallel(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, p), c -> c.join().toList()));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallel(Consumer<CollectingConfigurer> configurer, String... tags) {
            return new GenericCollector<>("parallel(e, p) [%s]".formatted(String.join(", ", tags)), (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, c -> { configurer.accept(c); c.parallelism(p); }), c -> c.join().toList()));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelWithExecutor() {
            return new GenericCollector<>("parallel(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e()).parallelism(p)), c -> c.join().toList()));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelToList() {
            return new GenericCollector<>("parallel(toList(), p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, p, toList()), CompletableFuture::join));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelToListWithExecutor() {
            return new GenericCollector<>("parallel(toList(), e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e()).parallelism(p), toList()), CompletableFuture::join));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelToListBatching() {
            return new GenericCollector<>("parallel(toList(), e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.batching().executor(e()).parallelism(p), toList()), CompletableFuture::join));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelBy() {
            return new GenericCollector<>("parallelBy(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, p), c -> ungrouped(c.join().toList())));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelByWithExecutor() {
            return new GenericCollector<>("parallelBy(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, c -> c.executor(e()).parallelism(p)), c -> ungrouped(c.join().toList())));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelByToList() {
            return new GenericCollector<>("parallelBy(toList(), p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, p, toList()), c -> ungrouped(c.join())));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelByToListWithExecutor() {
            return new GenericCollector<>("parallelBy(toList(), e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, c -> c.executor(e()).parallelism(p), toList()), c -> ungrouped(c.join())));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelToStream() {
            return new GenericCollector<>("parallelToStream(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, p), Stream::toList));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelToStreamWithExecutor() {
            return new GenericCollector<>("parallelToStream(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e()).parallelism(p)), Stream::toList));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelToStreamBatching() {
            return new GenericCollector<>("parallelToStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.batching().executor(e()).parallelism(p)), Stream::toList));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelToStreamBy() {
            return new GenericCollector<>("parallelToStreamBy(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, p), s -> s.map(Group::values).flatMap(Collection::stream).toList()));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelToStreamByWithExecutor() {
            return new GenericCollector<>("parallelToStreamBy(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, c -> c.executor(e()).parallelism(p)), ungrouped()));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelToOrderedStream() {
            return new GenericCollector<>("parallelToOrderedStream(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.ordered().parallelism(p)), Stream::toList));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelToOrderedStreamWithExecutor() {
            return new GenericCollector<>("parallelToOrderedStream(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.ordered().executor(e()).parallelism(p)), Stream::toList));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelToOrderedStreamBatching() {
            return new GenericCollector<>("parallelToOrderedStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.ordered().batching().executor(e()).parallelism(p)), Stream::toList));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelToOrderedStreamBy() {
            return new GenericCollector<>("parallelToOrderedStreamBy(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, c -> c.parallelism(p).ordered()), ungrouped()));
        }

        static GenericCollector<CollectorFactoryWithParallelism<Integer, Integer>> parallelToOrderedStreamByWithExecutor() {
            return new GenericCollector<>("parallelToOrderedStreamBy(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, c -> c.ordered().executor(e()).parallelism(p)), ungrouped()));
        }
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

        static GenericCollector<AsyncCollectorFactory<Integer, Integer>> parallel() {
            return new GenericCollector<>("parallel()", f -> collectingAndThen(ParallelCollectors.parallel(f), c -> c.thenApply(Stream::toList)));
        }

        static GenericCollector<AsyncCollectorFactory<Integer, Integer>> parallelToList() {
            return new GenericCollector<>("parallel(toList())", f -> ParallelCollectors.parallel(f, toList()));
        }

        static GenericCollector<AsyncCollectorFactory<Integer, Integer>> parallelToList(Consumer<CollectingConfigurer> configurer, String... tags) {
            return new GenericCollector<>("parallel(toList()) [%s]".formatted(String.join(", ", tags)), f -> ParallelCollectors.parallel(f, configurer, toList()));
        }
    }

    @FunctionalInterface
    interface GroupingCollectorFactory<T, R> {
        Collector<T, ?, List<Group<T, R>>> collector(Function<T, R> f);

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelBy(Function<Integer, Integer> classifier) {
            return new GenericCollector<>("parallelBy()", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f), c -> c.join().toList()));
        }

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelByWithExecutor(Function<Integer, Integer> classifier) {
            return new GenericCollector<>("parallelBy(e)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, c -> c.executor(e())), c -> c.join().toList()));
        }

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelBy(Function<Integer, Integer> classifier, int parallelism) {
            return new GenericCollector<>("parallelBy(p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, parallelism), c -> c.join().toList()));
        }

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelByWithExecutor(Function<Integer, Integer> classifier, int parallelism) {
            return new GenericCollector<>("parallelBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, c -> c.executor(e()).parallelism(parallelism)), c -> c.join().toList()));
        }

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelByToList(Function<Integer, Integer> classifier) {
            return new GenericCollector<>("parallelBy(toList())", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, toList()), c -> c.join()));
        }

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelByToList(Function<Integer, Integer> classifier, int parallelism) {
            return new GenericCollector<>("parallelBy(toList(), p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, parallelism, toList()), c -> c.join()));
        }

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelByToListWithExecutor(Function<Integer, Integer> classifier) {
            return new GenericCollector<>("parallelBy(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, c -> c.executor(e()), toList()), c -> c.join()));
        }

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelByToListWithExecutor(Function<Integer, Integer> classifier, int parallelism) {
            return new GenericCollector<>("parallelBy(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, c -> c.executor(e()).parallelism(parallelism), toList()), c -> c.join()));
        }

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelToStreamBy(Function<Integer, Integer> classifier) {
            return new GenericCollector<>("parallelToStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f), c -> c.toList()));
        }

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelToStreamByWithExecutor(Function<Integer, Integer> classifier) {
            return new GenericCollector<>("parallelToStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.executor(e())), c -> c.toList()));
        }

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelToStreamByWithExecutor(Function<Integer, Integer> classifier, int parallelism) {
            return new GenericCollector<>("parallelToStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.executor(e()).parallelism(parallelism)), c -> c.toList()));
        }

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelToOrderedStreamBy(Function<Integer, Integer> classifier) {
            return new GenericCollector<>("parallelToOrderedStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered()), c -> c.toList()));
        }

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelToOrderedStreamBy(Function<Integer, Integer> classifier, int parallelism) {
            return new GenericCollector<>("parallelToOrderedStreamBy(p)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered().parallelism(parallelism)), c -> c.toList()));
        }

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelToOrderedStreamByWithExecutor(Function<Integer, Integer> classifier) {
            return new GenericCollector<>("parallelToOrderedStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered().executor(e())), c -> c.toList()));
        }

        static GenericCollector<GroupingCollectorFactory<Integer, Integer>> parallelToOrderedStreamByWithExecutor(Function<Integer, Integer> classifier, int parallelism) {
            return new GenericCollector<>("parallelToOrderedStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, c -> c.ordered().executor(e()).parallelism(parallelism)), c -> c.toList()));
        }
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

    private static <K, V> Function<Stream<Group<K, V>>, List<V>> ungrouped() {
        return s -> s.flatMap(g -> g.values().stream()).toList();
    }

    private static <K, V> List<V> ungrouped(Collection<Group<K, V>> collection) {
        return collection.stream().flatMap(g -> g.values().stream()).toList();
    }

    private static <K, V> List<V> ungrouped(CompletableFuture<Collection<Group<K, V>>> stream) {
        return stream.join().stream().flatMap(g -> g.values().stream()).toList();
    }
}
