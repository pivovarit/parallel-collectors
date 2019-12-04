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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.pivovarit.collectors.AsyncParallelPartitioningCollector.partitioned;
import static com.pivovarit.collectors.AsyncParallelPartitioningCollector.requireValidParallelism;
import static java.lang.Runtime.getRuntime;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * @author Grzegorz Piwowarek
 */
class ParallelStreamPartitioningCollector<T, R> implements Collector<List<T>, List<CompletableFuture<List<R>>>, Stream<R>> {

    private final Dispatcher<List<R>> dispatcher;
    private final Function<T, R> mapper;
    private final Function<List<CompletableFuture<List<R>>>, Stream<R>> processor;
    private final Set<Characteristics> characteristics;

    private ParallelStreamPartitioningCollector(
      Function<T, R> mapper,
      Function<List<CompletableFuture<List<R>>>, Stream<R>> processor,
      Set<Characteristics> characteristics,
      Executor executor, int parallelism) {
        this.processor = processor;
        this.characteristics = characteristics;
        this.dispatcher = Dispatcher.unbounded(executor);
        this.mapper = mapper;
    }

    private ParallelStreamPartitioningCollector(
      Function<T, R> mapper,
      Function<List<CompletableFuture<List<R>>>, Stream<R>> processor,
      Set<Characteristics> characteristics,
      Executor executor) {
        this.characteristics = characteristics;
        this.dispatcher = Dispatcher.unbounded(executor);
        this.mapper = mapper;
        this.processor = processor;
    }

    private void startConsuming() {
        if (!dispatcher.isRunning()) {
            dispatcher.start();
        }
    }

    @Override
    public Supplier<List<CompletableFuture<List<R>>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<CompletableFuture<List<R>>>, List<T>> accumulator() {
        return (acc, chunk) -> {
            startConsuming();
            acc.add(dispatcher.enqueue(() -> chunk.stream().map(mapper).collect(toList())));
        };
    }

    @Override
    public BinaryOperator<List<CompletableFuture<List<R>>>> combiner() {
        return (left, right) -> {
            left.addAll(right);
            return left;
        };
    }

    @Override
    public Function<List<CompletableFuture<List<R>>>, Stream<R>> finisher() {
        return processor
          .compose(i -> {
              dispatcher.stop();
              return i;
          });
    }

    @Override
    public Set<Characteristics> characteristics() {
        return characteristics;
    }

    static <T, R> Collector<T, ?, Stream<R>> streaming(Function<T, R> mapper, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return Collectors.collectingAndThen(toList(), list -> partitioned(list, getDefaultParallelism())
          .collect(new ParallelStreamPartitioningCollector<>(mapper, streamInCompletionOrderStrategy(), Collections
            .emptySet(), executor)));
    }

    static <T, R> Collector<T, ?, Stream<R>> streaming(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);
        return Collectors.collectingAndThen(toList(), list -> partitioned(list, parallelism)
          .collect(new ParallelStreamPartitioningCollector<>(mapper, streamInCompletionOrderStrategy(), Collections
            .emptySet(), executor)));
    }

    static <T, R> Collector<T, ?, Stream<R>> streamingOrdered(Function<T, R> mapper, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return Collectors.collectingAndThen(toList(), list -> partitioned(list, getDefaultParallelism())
          .collect(new ParallelStreamPartitioningCollector<>(mapper, streamOrderedStrategy(), Collections
            .emptySet(), executor)));
    }

    static <T, R> Collector<T, ?, Stream<R>> streamingOrdered(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);
        return Collectors.collectingAndThen(toList(), list -> partitioned(list, parallelism)
          .collect(new ParallelStreamPartitioningCollector<>(mapper, streamOrderedStrategy(), Collections
            .emptySet(), executor)));
    }

    private static <R> Function<List<CompletableFuture<List<R>>>, Stream<R>> streamInCompletionOrderStrategy() {
        return futures -> StreamSupport.stream(new CompletionOrderBatchingSpliterator<>(futures), false);
    }

    private static <R> Function<List<CompletableFuture<List<R>>>, Stream<R>> streamOrderedStrategy() {
        return futures -> futures.stream().flatMap(f -> f.join().stream());
    }

    private static int getDefaultParallelism() {
        return Math.max(getRuntime().availableProcessors() - 1, 1);
    }
}