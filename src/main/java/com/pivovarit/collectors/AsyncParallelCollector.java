package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.Dispatcher.unbounded;
import static java.lang.Runtime.getRuntime;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncParallelCollector<T, R, C>
  implements Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>> {

    private final Dispatcher<R> dispatcher;
    private final Function<T, R> mapper;
    private final Function<CompletableFuture<Stream<R>>, CompletableFuture<C>> processor;

    private final CompletableFuture<C> result = new CompletableFuture<>();

    private AsyncParallelCollector(
      Function<T, R> mapper,
      Dispatcher<R> dispatcher,
      Function<CompletableFuture<Stream<R>>, CompletableFuture<C>> processor) {
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
            startConsuming();
            acc.add(dispatcher.enqueue(() -> mapper.apply(e)));
        };
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        return futures -> {
            dispatcher.stop();
            processor.apply(toCombined(futures)).handle(result());
            return result;
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }

    private static <T> CompletableFuture<Stream<T>> toCombined(List<CompletableFuture<T>> futures) {
        return allOf(futures)
          .thenApply(__ -> futures.stream()
            .map(CompletableFuture::join));
    }

    private void startConsuming() {
        if (!dispatcher.isRunning()) {
            dispatcher.start().handle((__, ex) -> result.completeExceptionally(ex));
        }
    }

    private static <T> CompletableFuture<Void> allOf(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        futures.forEach(f -> f.handle((__, ex) -> ex != null && future.completeExceptionally(ex)));

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(future::complete);

        return future;
    }

    private BiFunction<C, Throwable, Object> result() {
        return (c, ex) -> ex == null ? result.complete(c) : result.completeExceptionally(ex);
    }

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collectingToStream(Function<T, R> mapper, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return new AsyncParallelCollector<>(mapper, Dispatcher.limiting(executor), t -> t);
    }

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collectingToStream(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);
        return new AsyncParallelCollector<>(mapper, Dispatcher.limiting(executor, parallelism), t -> t);
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> collectingWithCollector(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor) {
        requireNonNull(collector, "collector can't be null");
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return new AsyncParallelCollector<>(mapper, Dispatcher.limiting(executor), r -> r
          .thenApply(s -> s.collect(collector)));
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> collectingWithCollector(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(collector, "collector can't be null");
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);
        return new AsyncParallelCollector<>(mapper, Dispatcher.limiting(executor, parallelism), r -> r
          .thenApply(s -> s.collect(collector)));
    }

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collectingToStreamInBatches(Function<T, R> mapper, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return collectingToStreamInBatches(mapper, executor, getDefaultParallelism());
    }

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collectingToStreamInBatches(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);

        return collectingAndThen(toList(), list -> partitioned(list, parallelism)
          .collect(new AsyncParallelCollector<>(batch(mapper), unbounded(executor), cf -> cf
            .thenApply(s -> s.flatMap(Collection::stream)))));
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> collectingWithCollectorInBatches(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor) {
        requireNonNull(collector, "collector can't be null");
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return collectingWithCollectorInBatches(collector, mapper, executor, getDefaultParallelism());
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> collectingWithCollectorInBatches(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(collector, "collector can't be null");
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);

        return batching(collector, mapper, executor, parallelism);
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> batching(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor, int parallelism) {
        return collectingAndThen(toList(), list -> partitioned(list, parallelism)
          .collect(new AsyncParallelCollector<>(batch(mapper),
            unbounded(executor), cf -> cf.thenApply(s -> s.flatMap(Collection::stream).collect(collector)))));
    }

    static <T, R> Function<List<T>, List<R>> batch(Function<T, R> mapper) {
        return batch -> batch.stream().map(mapper).collect(toList());
    }

    static <T> Stream<List<T>> partitioned(List<T> list, int numberOfParts) {
        Stream.Builder<List<T>> builder = Stream.builder();
        int size = list.size();
        int chunkSize = (int) Math.ceil(((double) size) / numberOfParts);
        int leftElements = size;
        int i = 0;
        while (i < size && numberOfParts != 0) {
            builder.add(list.subList(i, i + chunkSize));
            i = i + chunkSize;
            leftElements = leftElements - chunkSize;
            chunkSize = (int) Math.ceil(((double) leftElements) / --numberOfParts);
        }
        return builder.build();
    }

    private static int getDefaultParallelism() {
        return Math.max(getRuntime().availableProcessors() - 1, 1);
    }

    static void requireValidParallelism(int parallelism) {
        if (parallelism < 1) {
            throw new IllegalArgumentException("Parallelism can't be lower than 1");
        }
    }
}
