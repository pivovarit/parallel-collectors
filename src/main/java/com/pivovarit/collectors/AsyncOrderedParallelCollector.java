package com.pivovarit.collectors;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncOrderedParallelCollector<T, R, C extends Collection<R>>
  extends AbstractOrderedParallelCollector<T, R, CompletableFuture<C>>
  implements AutoCloseable {

    private final Dispatcher<Map.Entry<Integer, R>> dispatcher;

    private final Function<T, R> operation;
    private final Supplier<C> collectionFactory;

    private final AtomicInteger seq = new AtomicInteger();

    AsyncOrderedParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor,
      int parallelism) {
        this.dispatcher = new ThrottlingDispatcher<>(executor, parallelism);
        this.collectionFactory = collection;
        this.operation = operation;
    }

    AsyncOrderedParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor) {
        this.dispatcher = new UnboundedDispatcher<>(executor);
        this.collectionFactory = collection;
        this.operation = operation;
    }

    @Override
    public BiConsumer<List<CompletableFuture<Map.Entry<Integer, R>>>, T> accumulator() {
        return (acc, e) -> {
            int nextVal = seq.getAndIncrement();
            acc.add(dispatcher.enqueue(() -> new AbstractMap.SimpleEntry<>(nextVal, operation.apply(e))));
        };
    }

    @Override
    public Function<List<CompletableFuture<Map.Entry<Integer, R>>>, CompletableFuture<C>> finisher() {
        if (dispatcher.getWorkingQueue().size() != 0) {
            dispatcher.start();
            return foldLeftFuturesOrdered(collectionFactory)
              .andThen(f -> supplyWithResources(() -> f, dispatcher::close));
        } else {
            return supplyWithResources(() -> (__) -> completedFuture(collectionFactory
              .get()), dispatcher::close);
        }
    }

    @Override
    public Set<Collector.Characteristics> characteristics() {
        return new HashSet<>();
    }

    @Override
    public void close() {
        dispatcher.close();
    }
}
