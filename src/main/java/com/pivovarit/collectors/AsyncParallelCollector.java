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
import java.util.Collections;
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

import static com.pivovarit.collectors.BatchingSpliterator.partitioned;
import static java.util.concurrent.CompletableFuture.allOf;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncParallelCollector<T, R, C> extends AbstractParallelCollector<T, R, CompletableFuture<C>> {

    private final Function<Stream<R>, C> finalizer;

    AsyncParallelCollector(Function<? super T, ? extends R> task, Dispatcher<R> dispatcher, Function<Stream<R>, C> finalizer) {
        super(task, dispatcher);
        this.finalizer = finalizer;
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finalizer() {
        return futures -> {
            var combined = allOf(futures.toArray(CompletableFuture[]::new))
              .thenApply(__ -> futures.stream().map(CompletableFuture::join));

            for (var future : futures) {
                future.whenComplete((o, ex) -> {
                    if (ex != null) {
                        combined.completeExceptionally(ex);
                    }
                });
            }

            return combined.thenApply(finalizer);
        };
    }

    record BatchingCollector<T, R, C>(Function<? super T, ? extends R> task, Function<Stream<R>, C> finalizer,
                                      Executor executor, int parallelism)
      implements Collector<T, ArrayList<T>, CompletableFuture<C>> {

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
        public Function<ArrayList<T>, CompletableFuture<C>> finisher() {
            return items -> {
                if (items.size() == parallelism) {
                    return items.stream()
                      .collect((Collector<T, ?, CompletableFuture<C>>) new AsyncParallelCollector<T, R, C>(task, new Dispatcher<>(executor, parallelism), finalizer));
                } else {
                    return partitioned(items, parallelism)
                      .collect((Collector<List<T>, ?, CompletableFuture<C>>) new AsyncParallelCollector<List<T>, List<R>, C>((Function<? super List<T>, ? extends List<R>>) batch -> {
                          List<R> list = new ArrayList<>(batch.size());
                          for (T t : batch) {
                              list.add(task.apply(t));
                          }
                          return list;
                      }, new Dispatcher<>(executor, parallelism), r -> finalizer.apply(r.flatMap(Collection::stream))));
                }
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.emptySet();
        }
    }
}
