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
import java.util.concurrent.Executor;
import java.util.function.Consumer;
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
      Consumer<CollectingConfigurer> configurer) {
        return Factory.collectingBy(classifier, (Function<Stream<Grouped<K, R>>, Stream<Grouped<K, R>>>) i -> i, mapper, configurer);
    }

    static <T, K, R, C> Collector<T, ?, CompletableFuture<C>> collectingBy(
      Function<? super T, ? extends K> classifier,
      Function<Stream<Grouped<K, R>>, C> finalizer,
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer) {
        Objects.requireNonNull(classifier, "classifier cannot be null");
        Objects.requireNonNull(finalizer, "finalizer cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(configurer, "configurer cannot be null");

        // evaluate eagerly
        configurer.accept(new CollectingConfigurer());

        return Collectors.collectingAndThen(
          Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList()),
          groups -> groups.entrySet()
            .stream()
            .collect(collecting(finalizer,
              e -> new Grouped<>(e.getKey(), e.getValue().stream()
                .map(mapper.andThen(a -> (R) a))
                .toList()), configurer))
        );
    }

    static <T, R, C> Collector<T, ?, CompletableFuture<C>> collecting(
      Function<Stream<R>, C> finalizer,
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer
    ) {
        requireNonNull(mapper, "mapper can't be null");
        requireNonNull(mapper, "configurer can't be null");

        var config = ConfigProcessor.process(collecting(configurer).getConfig());

        return select(mapper, config, new ModeFactory<T, R, CompletableFuture<C>>() {
            @Override
            public Collector<T, ?, CompletableFuture<C>> async(Function<? super T, ? extends R> mapper, Executor executor) {
                return new AsyncCollector<>(mapper, finalizer, executor);
            }

            @Override
            public Collector<T, ?, CompletableFuture<C>> batching(Function<? super T, ? extends R> mapper, Executor executor, int parallelism) {
                return new AsyncParallelCollector.BatchingCollector<>(mapper, finalizer, executor, parallelism);
            }

            @Override
            public Collector<T, ?, CompletableFuture<C>> parallel(Function<? super T, ? extends R> mapper, Dispatcher<R> dispatcher) {
                return new AsyncParallelCollector<>(mapper, dispatcher, finalizer);
            }
        });
    }

    static <T, K, R> Collector<T, ?, Stream<Grouped<K, R>>> streamingBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Consumer<StreamingConfigurer> configurer) {
        Objects.requireNonNull(classifier, "classifier cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(configurer, "configurer cannot be null");

        // evaluate eagerly
        configurer.accept(new StreamingConfigurer());

        return Collectors.collectingAndThen(
          Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList()),
          groups -> groups.entrySet()
            .stream()
            .collect(streaming(e -> new Grouped<>(e.getKey(), e.getValue().stream()
              .map(mapper.andThen(a -> (R) a))
              .toList()), configurer))
        );
    }

    static <T, R> Collector<T, ?, Stream<R>> streaming(
      Function<? super T, ? extends R> mapper,
      Consumer<StreamingConfigurer> configurer) {
        requireNonNull(mapper, "mapper can't be null");
        requireNonNull(mapper, "configurer can't be null");

        var config = ConfigProcessor.process(streaming(configurer).getConfig());

        return select(mapper, config, new ModeFactory<T, R, Stream<R>>() {
            @Override
            public Collector<T, ?, Stream<R>> async(Function<? super T, ? extends R> m, Executor ex) {
                return new SyncCollector<>(m);
            }

            @Override
            public Collector<T, ?, Stream<R>> batching(Function<? super T, ? extends R> m, Executor ex, int p) {
                return new AsyncParallelStreamingCollector.BatchingCollector<>(m, ex, p, config.ordered());
            }

            @Override
            public Collector<T, ?, Stream<R>> parallel(Function<? super T, ? extends R> m, Dispatcher<R> d) {
                return new AsyncParallelStreamingCollector<>(m, d, config.ordered());
            }
        });
    }

    private static <T, R, C> Collector<T, ?, C> select(
      Function<? super T, ? extends R> mapper,
      ConfigProcessor.Config config,
      ModeFactory<T, R, C> factory
    ) {
        if (config.parallelism() == 1) {
            return factory.async(mapper, config.executor());
        }

        if (config.batching()) {
            if (config.parallelism() == 0) {
                throw new IllegalArgumentException("it's obligatory to provide parallelism when using batching");
            }
            return factory.batching(mapper, config.executor(), config.parallelism());
        }

        var dispatcher = (config.parallelism() > 0)
          ? new Dispatcher<R>(config.executor(), config.parallelism())
          : new Dispatcher<R>(config.executor());

        return factory.parallel(mapper, dispatcher);
    }

    interface ModeFactory<T, R, C> {
        Collector<T, ?, C> async(Function<? super T, ? extends R> mapper, Executor executor);

        Collector<T, ?, C> batching(Function<? super T, ? extends R> mapper, Executor executor, int parallelism);

        Collector<T, ?, C> parallel(Function<? super T, ? extends R> mapper, Dispatcher<R> dispatcher);
    }

    static StreamingConfigurer streaming(Consumer<StreamingConfigurer> consumer) {
        var c = new StreamingConfigurer();
        consumer.accept(c);
        return c;
    }

    static CollectingConfigurer collecting(Consumer<CollectingConfigurer> consumer) {
        var c = new CollectingConfigurer();
        consumer.accept(c);
        return c;
    }
}
