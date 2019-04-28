package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * @author Grzegorz Piwowarek
 */
abstract class AbstractSyncStreamCollector<T, R> implements Collector<T, List<CompletableFuture<R>>, Stream<R>> {

    private final Dispatcher<R> dispatcher;
    private final Function<T, R> function;
    private final Function<List<CompletableFuture<R>>, Stream<R>> processor;

    AbstractSyncStreamCollector(
      Function<T, R> function,
      Function<List<CompletableFuture<R>>, Stream<R>> processor,
      Executor executor, int parallelism) {
        this.processor = processor;
        this.dispatcher = new Dispatcher<>(executor, parallelism);
        this.function = function;
    }

    AbstractSyncStreamCollector(
      Function<T, R> function,
      Function<List<CompletableFuture<R>>, Stream<R>> processor,
      Executor executor) {
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
}
