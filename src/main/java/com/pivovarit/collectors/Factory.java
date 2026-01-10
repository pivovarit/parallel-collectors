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

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

final class Factory {

    private Factory() {
    }

    static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Grouped<K, R>>>> collectingBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Options.CollectingOption... options) {
        return Factory.collectingBy(classifier, (Function<Stream<Grouped<K, R>>, Stream<Grouped<K, R>>>) i -> i, mapper, options);
    }

    static <T, K, R, C> Collector<T, ?, CompletableFuture<C>> collectingBy(
      Function<? super T, ? extends K> classifier,
      Function<Stream<Grouped<K, R>>, C> finalizer,
      Function<? super T, ? extends R> mapper,
      Options.CollectingOption... options) {
        Objects.requireNonNull(classifier, "classifier cannot be null");
        Objects.requireNonNull(finalizer, "finalizer cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(options, "options cannot be null");

        return Collectors.collectingAndThen(
          Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList()),
          groups -> groups.entrySet()
            .stream()
            .collect(collecting(finalizer,
              e -> new Grouped<>(e.getKey(), e.getValue().stream()
                .map(mapper.andThen(a -> (R) a))
                .toList()), options))
        );
    }

    static <T, R, C> Collector<T, ?, CompletableFuture<C>> collecting(
      Function<Stream<R>, C> finalizer,
      Function<? super T, ? extends R> mapper,
      Options.CollectingOption... options) {
        requireNonNull(mapper, "mapper can't be null");

        var config = ConfigProcessor.process(options);

        if (config.parallelism() == 1) {
            return new AsyncCollector<>(mapper, finalizer, config.executor());
        }

        if (config.batching()) {
            if (config.parallelism() == 0) {
                throw new IllegalArgumentException("it's obligatory to provide parallelism when using batching");
            }

            return new AsyncParallelCollector.BatchingCollector<>(mapper, finalizer, config.executor(), config.parallelism());
        }

        return config.parallelism() > 0
          ? AsyncParallelCollector.from(mapper, finalizer, config.executor(), config.parallelism())
          : AsyncParallelCollector.from(mapper, finalizer, config.executor());
    }

    static <T, K, R> Collector<T, ?, Stream<Grouped<K, R>>> streamingBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Options.StreamingOption... options) {
        Objects.requireNonNull(classifier, "classifier cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(options, "options cannot be null");

        return Collectors.collectingAndThen(
          Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList()),
          groups -> groups.entrySet()
            .stream()
            .collect(streaming(e -> new Grouped<>(e.getKey(), e.getValue().stream()
              .map(mapper.andThen(a -> (R) a))
              .toList()), options))
        );
    }

    static <T, R> Collector<T, ?, Stream<R>> streaming(Function<? super T, ? extends R> mapper, Options.StreamingOption... options) {
        requireNonNull(mapper, "mapper can't be null");

        var config = ConfigProcessor.process(options);

        if (config.parallelism() == 1) {
            return new SyncCollector<>(mapper);
        }

        if (config.batching()) {
            if (config.parallelism() == 0) {
                throw new IllegalArgumentException("it's obligatory to provide parallelism when using batching");
            }

            return new AsyncParallelStreamingCollector.BatchingCollector<>(mapper, config.executor(), config.parallelism(), config.ordered());
        }

        return config.parallelism() > 0
          ? AsyncParallelStreamingCollector.from(mapper, config.executor(), config.parallelism(), config.ordered())
          : AsyncParallelStreamingCollector.from(mapper, config.executor(), config.ordered());
    }
}
