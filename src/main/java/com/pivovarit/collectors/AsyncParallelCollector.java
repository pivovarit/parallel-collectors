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

import static com.pivovarit.collectors.BatchingStream.batching;
import static com.pivovarit.collectors.BatchingStream.partitioned;
import static com.pivovarit.collectors.Dispatcher.getDefaultParallelism;
import static com.pivovarit.collectors.Dispatcher.of;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncParallelCollector<T, R, C>
  implements Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>> {

    private final Dispatcher<R> dispatcher;
    private final Function<T, R> mapper;
    private final Function<Stream<R>, C> processor;

    private final CompletableFuture<C> result = new CompletableFuture<>();

    private AsyncParallelCollector(
      Function<T, R> mapper,
      Dispatcher<R> dispatcher,
      Function<Stream<R>, C> processor) {
        this.dispatcher = dispatcher;
        this.processor = processor;
        this.mapper = mapper;
    }

    @Override
    public Supplier<List<CompletableFuture<R>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BinaryOperator<List<CompletableFuture<R>>> combiner() {
        return (left, right) -> {
            throw new UnsupportedOperationException();
        };
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> {
            if (!dispatcher.isRunning()) {
                dispatcher.start();
            }
            acc.add(dispatcher.enqueue(() -> mapper.apply(e)));
        };
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        return futures -> {
            dispatcher.stop();

            return toCombined(futures)
              .thenApply(processor)
              .handle((c, ex) -> ex == null ? result.complete(c) : result.completeExceptionally(ex))
              .thenCompose(__ -> result);
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }

    private static <T> CompletableFuture<Stream<T>> toCombined(List<CompletableFuture<T>> futures) {
        CompletableFuture<Stream<T>> combined = allOf(futures.toArray(new CompletableFuture[0]))
          .thenApply(__ -> futures.stream()
            .map(CompletableFuture::join));

        for (CompletableFuture<T> f : futures) {
            f.exceptionally(ex -> {
                combined.completeExceptionally(ex);
                return null;
            });
        }

        return combined;
    }

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collectingToStream(Function<T, R> mapper, Executor executor) {
        return collectingToStream(mapper, executor, getDefaultParallelism());
    }

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collectingToStream(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);

        return parallelism == 1
          ? asyncCollector(mapper, executor, i -> i)
          : new AsyncParallelCollector<>(mapper, Dispatcher.limiting(executor, parallelism), t -> t);
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> collectingWithCollector(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor) {
        return collectingWithCollector(collector, mapper, executor, getDefaultParallelism());
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> collectingWithCollector(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(collector, "collector can't be null");
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);

        return parallelism == 1
          ? asyncCollector(mapper, executor, s -> s.collect(collector))
          : new AsyncParallelCollector<>(mapper, Dispatcher.limiting(executor, parallelism), s -> s.collect(collector));
    }

    static void requireValidParallelism(int parallelism) {
        if (parallelism < 1) {
            throw new IllegalArgumentException("Parallelism can't be lower than 1");
        }
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> asyncCollector(Function<T, R> mapper, Executor executor, Function<Stream<R>, RR> finisher) {
        return collectingAndThen(toList(), list -> supplyAsync(() -> finisher.apply(list.stream().map(mapper).collect(toList()).stream()), executor));
    }

    static final class BatchingCollectors {

        private BatchingCollectors() {
        }

        static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> collectingWithCollector(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor, int parallelism) {
            requireNonNull(collector, "collector can't be null");
            requireNonNull(executor, "executor can't be null");
            requireNonNull(mapper, "mapper can't be null");
            requireValidParallelism(parallelism);

            return parallelism == 1
              ? asyncCollector(mapper, executor, s -> s.collect(collector))
              : batchingCollector(mapper, executor, parallelism, s -> s.collect(collector));
        }

        static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collectingToStream(
          Function<T, R> mapper,
          Executor executor, int parallelism) {
            requireNonNull(executor, "executor can't be null");
            requireNonNull(mapper, "mapper can't be null");
            requireValidParallelism(parallelism);

            return parallelism == 1
              ? asyncCollector(mapper, executor, i -> i)
              : batchingCollector(mapper, executor, parallelism, s -> s);
        }

        private static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> batchingCollector(Function<T, R> mapper, Executor executor, int parallelism, Function<Stream<R>, RR> finisher) {
            return collectingAndThen(
              toList(),
              list -> partitioned(list, parallelism).collect(
                new AsyncParallelCollector<>(batching(mapper), of(executor),
                  listStream -> finisher.apply(listStream.flatMap(Collection::stream)))));
        }
    }
}