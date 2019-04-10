package com.pivovarit.collectors;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.AbstractAsyncUnorderedParallelCollector.supplyWithResources;
import static java.util.concurrent.CompletableFuture.allOf;

/**
 * @author Grzegorz Piwowarek
 */
abstract class AbstractAsyncOrderedParallelCollector<T, R, C>
  implements Collector<T, List<CompletableFuture<Map.Entry<Integer, R>>>, CompletableFuture<C>>, AutoCloseable {

    private final Dispatcher<Map.Entry<Integer, R>> dispatcher;
    private final Function<T, R> function;

    private final AtomicInteger seq = new AtomicInteger();

    AbstractAsyncOrderedParallelCollector(
      Function<T, R> function,
      Executor executor,
      int parallelism) {
        this.dispatcher = new ThrottlingDispatcher<>(executor, parallelism);
        this.function = function;
    }

    AbstractAsyncOrderedParallelCollector(
      Function<T, R> function,
      Executor executor) {
        this.dispatcher = new UnboundedDispatcher<>(executor);
        this.function = function;
    }

    abstract Function<CompletableFuture<Stream<R>>, CompletableFuture<C>> resultsProcessor();

    @Override
    public Supplier<List<CompletableFuture<Map.Entry<Integer, R>>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BinaryOperator<List<CompletableFuture<Map.Entry<Integer, R>>>> combiner() {
        return (left, right) -> {
            left.addAll(right);
            return left;
        };
    }

    @Override
    public BiConsumer<List<CompletableFuture<Map.Entry<Integer, R>>>, T> accumulator() {
        return (acc, e) -> {
            int nextVal = seq.getAndIncrement();
            acc.add(dispatcher.enqueue(() -> new AbstractMap.SimpleEntry<>(nextVal, function.apply(e))));
        };
    }

    @Override
    public Function<List<CompletableFuture<Map.Entry<Integer, R>>>, CompletableFuture<C>> finisher() {
        if (!dispatcher.isEmpty()) {
            dispatcher.start();
            return resultsProcessor()
              .compose(combineResultsOrdered())
              .andThen(f -> supplyWithResources(() -> f, dispatcher::close));
        } else {
            return empty -> resultsProcessor()
              .compose(combineResultsOrdered())
              .apply(Collections.emptyList());
        }
    }


    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.noneOf(Characteristics.class);
    }

    @Override
    public void close() {
        dispatcher.close();
    }

    private static <R> Function<List<CompletableFuture<Map.Entry<Integer, R>>>, CompletableFuture<Stream<R>>> combineResultsOrdered() {
        return futures -> allOf(futures.toArray(new CompletableFuture<?>[0]))
          .thenApply(__ -> futures.stream()
            .map(CompletableFuture::join)
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .map(Map.Entry::getValue));
    }
}
