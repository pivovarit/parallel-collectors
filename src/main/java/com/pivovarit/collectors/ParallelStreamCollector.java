package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collections;
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

import static com.pivovarit.collectors.AsyncParallelCollector.requireValidParallelism;
import static java.util.Objects.requireNonNull;

/**
 * @author Grzegorz Piwowarek
 */
class ParallelStreamCollector<T, R> implements Collector<T, List<CompletableFuture<R>>, Stream<R>> {

    private final Dispatcher<R> dispatcher;
    private final Function<T, R> function;
    private final Function<List<CompletableFuture<R>>, Stream<R>> processor;
    private final Set<Characteristics> characteristics;

    private ParallelStreamCollector(
      Function<T, R> function,
      Function<List<CompletableFuture<R>>, Stream<R>> processor,
      Set<Characteristics> characteristics,
      Executor executor, int parallelism) {
        this.processor = processor;
        this.characteristics = characteristics;
        this.dispatcher = new Dispatcher<>(executor, parallelism);
        this.function = function;
    }

    private ParallelStreamCollector(
      Function<T, R> function,
      Function<List<CompletableFuture<R>>, Stream<R>> processor,
      Set<Characteristics> characteristics,
      Executor executor) {
        this.characteristics = characteristics;
        this.dispatcher = new Dispatcher<>(executor);
        this.function = function;
        this.processor = processor;
    }

    private void startConsuming() {
        if (!dispatcher.isRunning()) {
            dispatcher.start();
        }
    }

    @Override
    public Supplier<List<CompletableFuture<R>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> {
            startConsuming();
            acc.add(dispatcher.enqueue(() -> function.apply(e)));
        };
    }

    @Override
    public BinaryOperator<List<CompletableFuture<R>>> combiner() {
        return (left, right) -> {
            left.addAll(right);
            return left;
        };
    }

    @Override
    public Function<List<CompletableFuture<R>>, Stream<R>> finisher() {
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
        return new ParallelStreamCollector<>(mapper, streamInCompletionOrderStrategy(), EnumSet.of(Characteristics.UNORDERED), executor);

    }

    static <T, R> Collector<T, ?, Stream<R>> streaming(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);
        return new ParallelStreamCollector<>(mapper, streamInCompletionOrderStrategy(), EnumSet.of(Characteristics.UNORDERED), executor, parallelism);

    }

    static <T, R> Collector<T, ?, Stream<R>> streamingOrdered(Function<T, R> mapper, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return new ParallelStreamCollector<>(mapper, streamOrderedStrategy(), Collections.emptySet(), executor);
    }

    static <T, R> Collector<T, ?, Stream<R>> streamingOrdered(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);
        return new ParallelStreamCollector<>(mapper, streamOrderedStrategy(), Collections.emptySet(), executor, parallelism);
    }

    private static <R> Function<List<CompletableFuture<R>>, Stream<R>> streamInCompletionOrderStrategy() {
        return futures -> StreamSupport.stream(new CompletionOrderSpliterator<>(futures), false);
    }

    private static <R> Function<List<CompletableFuture<R>>, Stream<R>> streamOrderedStrategy() {
        return futures -> futures.stream().map(CompletableFuture::join);
    }
}
