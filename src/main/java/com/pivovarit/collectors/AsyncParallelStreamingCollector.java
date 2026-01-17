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
package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.pivovarit.collectors.BatchingSpliterator.batching;
import static com.pivovarit.collectors.BatchingSpliterator.partitioned;
import static java.util.Collections.emptySet;

/**
 * @author Grzegorz Piwowarek
 */
class AsyncParallelStreamingCollector<T, R> implements Collector<T, List<CompletableFuture<R>>, Stream<R>> {

    private static final EnumSet<Characteristics> UNORDERED_CHARACTERISTICS = EnumSet.of(Characteristics.UNORDERED);

    private final Function<? super T, ? extends R> function;

    private final CompletionStrategy completionStrategy;

    private final Dispatcher<R> dispatcher;

    private AsyncParallelStreamingCollector(
      Function<? super T, ? extends R> function,
      Dispatcher<R> dispatcher,
      boolean ordered) {
        this.completionStrategy = ordered
          ? CompletionStrategy.ORDERED
          : CompletionStrategy.UNORDERED;
        this.dispatcher = dispatcher;
        this.function = function;
    }

    public static <T, R> Collector<T, ?, Stream<R>> from(
      Function<? super T, ? extends R> function, Executor executor, int parallelism, boolean ordered) {
        return new AsyncParallelStreamingCollector<>(function, new Dispatcher<>(executor, parallelism), ordered);
    }

    public static <T, R> Collector<T, ?, Stream<R>> from(
      Function<? super T, ? extends R> function, Executor executor, boolean ordered) {
        return new AsyncParallelStreamingCollector<>(function, new Dispatcher<>(executor), ordered);
    }

    @Override
    public Supplier<List<CompletableFuture<R>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> {
            dispatcher.start();
            acc.add(dispatcher.submit(() -> function.apply(e)));
        };
    }

    @Override
    public BinaryOperator<List<CompletableFuture<R>>> combiner() {
        return (left, right) -> {
            throw new UnsupportedOperationException(
              "Using parallel stream with parallel collectors is a bad idea");
        };
    }

    @Override
    public Function<List<CompletableFuture<R>>, Stream<R>> finisher() {
        return acc -> {
            dispatcher.stop();
            return switch (completionStrategy) {
                case ORDERED -> acc.stream().map(CompletableFuture::join);
                case UNORDERED -> StreamSupport.stream(new CompletionOrderSpliterator<>(acc), false);
            };
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return switch (completionStrategy) {
            case ORDERED -> emptySet();
            case UNORDERED -> UNORDERED_CHARACTERISTICS;
        };
    }

    record BatchingCollector<T, R>(Function<? super T, ? extends R> task, Executor executor, int parallelism,
                                   boolean ordered)
      implements Collector<T, ArrayList<T>, Stream<R>> {

        @Override
        public Supplier<ArrayList<T>> supplier() {
            return ArrayList::new;
        }

        @Override
        public BiConsumer<ArrayList<T>, T> accumulator() {
            return ArrayList::add;
        }

        @Override
        public BinaryOperator<ArrayList<T>> combiner() {
            return (left, right) -> {
                throw new UnsupportedOperationException("using parallel stream with parallel collectors is not supported");
            };
        }

        @Override
        public Function<ArrayList<T>, Stream<R>> finisher() {
            return items -> {
                if (items.size() == parallelism) {
                    return items.stream()
                      .collect(new AsyncParallelStreamingCollector<>(task, new Dispatcher<>(executor, parallelism), ordered));
                } else {
                    return partitioned(items, parallelism)
                      .collect(new AsyncParallelStreamingCollector<>(batching(task), new Dispatcher<>(executor, parallelism), ordered))
                      .flatMap(Collection::stream);
                }
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            return emptySet();
        }
    }
}
