package com.pivovarit.collectors;

import java.util.ArrayList;
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

class SyncCompletionOrderParallelCollector<T, R> implements Collector<T, List<CompletableFuture<R>>, Stream<R>> {

    private final Dispatcher<R> dispatcher;
    private final Function<T, R> function;

    SyncCompletionOrderParallelCollector(
      Function<T, R> function,
      Executor executor,
      int parallelism) {
        this.dispatcher = new Dispatcher<>(executor, parallelism);
        this.function = function;
    }

    SyncCompletionOrderParallelCollector(
      Function<T, R> function,
      Executor executor) {
        this.dispatcher = new Dispatcher<>(executor);
        this.function = function;
    }

    @Override
    public Supplier<List<CompletableFuture<R>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> acc.add(dispatcher.enqueue(() -> function.apply(e)));
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
        if (!dispatcher.isEmpty()) {
            dispatcher.start();
            return futures -> futures.stream()
              .map(CompletableFuture::join);
        } else {
            return __ -> Stream.empty();
        }
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }
}
