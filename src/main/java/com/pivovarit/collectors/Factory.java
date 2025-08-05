package com.pivovarit.collectors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

final class Factory {

    private Factory() {
    }

    static <T, R, C> Collector<T, ?, CompletableFuture<C>> collecting(Function<Stream<R>, C> finalizer, Function<? super T, ? extends R> mapper, Options.CollectingOption... options) {
        requireNonNull(mapper, "mapper can't be null");

        var config = ConfigProcessor.process(options);

        var batching = config.batching().orElse(false);
        var executor = config.executor().orElseGet(defaultExecutor());

        if (config.parallelism().orElse(-1) == 1) {
            return new AsyncCollector<>(mapper, finalizer, executor);
        }

        if (batching) {
            var parallelism = config.parallelism().orElseThrow(() -> new IllegalArgumentException("it's obligatory to provide parallelism when using batching"));
            return new AsyncParallelCollector.BatchingCollector<>(mapper, finalizer, executor, parallelism);
        }

        return config.parallelism().isPresent()
          ? AsyncParallelCollector.from(mapper, finalizer, executor, config.parallelism().getAsInt())
          : AsyncParallelCollector.from(mapper, finalizer, executor);
    }

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collecting(Function<? super T, ? extends R> mapper, Options.CollectingOption... options) {
        Function<Stream<R>, Stream<R>> finalizer = i -> i;
        return collecting(finalizer, mapper, options);
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
            var parallelism = config.parallelism().orElseThrow(() -> new IllegalArgumentException("it's obligatory to provide parallelism when using batching"));
            return new AsyncParallelStreamingCollector.BatchingCollector<>(mapper, executor, parallelism, ordered);
        }

        return config.parallelism().isPresent()
          ? AsyncParallelStreamingCollector.from(mapper, executor, config.parallelism().getAsInt(), ordered)
          : AsyncParallelStreamingCollector.from(mapper, executor, ordered);
    }

    private static Supplier<Executor> defaultExecutor() {
        return Executors::newVirtualThreadPerTaskExecutor;
    }
}
