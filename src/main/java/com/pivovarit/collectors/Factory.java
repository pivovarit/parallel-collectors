package com.pivovarit.collectors;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
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
        Objects.requireNonNull(classifier, "classifier cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(options, "options cannot be null");

        return Collectors.collectingAndThen(
          Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList()),
          groups -> groups.entrySet()
            .stream()
            .collect(collecting(e -> new Grouped<>(e.getKey(), e.getValue().stream()
              .map(mapper.andThen(a -> (R) a))
              .toList()), options))
        );
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

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collecting(
      Function<? super T, ? extends R> mapper,
      Options.CollectingOption... options) {
        requireNonNull(mapper, "mapper can't be null");

        return collecting((Function<Stream<R>, Stream<R>>) i -> i, mapper, options);
    }

    static <T, R, C> Collector<T, ?, CompletableFuture<C>> collecting(
      Function<Stream<R>, C> finalizer,
      Function<? super T, ? extends R> mapper,
      Options.CollectingOption... options) {
        requireNonNull(mapper, "mapper can't be null");

        var config = ConfigProcessor.process(options);

        var batching = config.batching().orElse(false);
        var executor = config.executor().orElseGet(defaultExecutor());

        if (config.parallelism().orElse(-1) == 1) {
            return new AsyncCollector<>(mapper, finalizer, executor);
        }

        if (batching) {
            var parallelism = config.parallelism()
              .orElseThrow(() -> new IllegalArgumentException("it's obligatory to provide parallelism when using batching"));
            return new AsyncParallelCollector.BatchingCollector<>(mapper, finalizer, executor, parallelism);
        }

        return config.parallelism().isPresent()
          ? AsyncParallelCollector.from(mapper, finalizer, executor, config.parallelism().getAsInt())
          : AsyncParallelCollector.from(mapper, finalizer, executor);
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
        boolean batching = config.batching().orElse(false);
        boolean ordered = config.ordered().orElse(false);
        var executor = config.executor().orElseGet(defaultExecutor());

        if (config.parallelism().orElse(-1) == 1) {
            return new SyncCollector<>(mapper);
        }

        if (batching) {
            var parallelism = config.parallelism()
              .orElseThrow(() -> new IllegalArgumentException("it's obligatory to provide parallelism when using batching"));
            return new AsyncParallelStreamingCollector.BatchingCollector<>(mapper, executor, parallelism, ordered);
        }

        return config.parallelism().isPresent()
          ? AsyncParallelStreamingCollector.from(mapper, executor, config.parallelism().getAsInt(), ordered)
          : AsyncParallelStreamingCollector.from(mapper, executor, ordered);
    }

    private static Supplier<Executor> defaultExecutor() {
        return () -> Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
          .name("parallel-collectors-", 0)
          .factory());
    }
}
