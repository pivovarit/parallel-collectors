package com.pivovarit.collectors;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * @author Grzegorz Piwowarek
 */
final class ThrottlingParallelCollector<T, R, C extends Collection<R>>
  extends AbstractParallelCollector<T, R, C>
  implements AutoCloseable {

    private final Dispatcher<R> dispatcher;

    private final Function<T, R> operation;
    private final Supplier<C> collectionFactory;

    ThrottlingParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor,
      int parallelism) {
        this(operation, collection, executor, new ConcurrentLinkedQueue<>(), new ConcurrentLinkedQueue<>(), parallelism);
    }

    ThrottlingParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor,
      Queue<Supplier<R>> workingQueue,
      Queue<CompletableFuture<R>> pendingQueue,
      int parallelism) {
        this.dispatcher = new ThrottlingDispatcher<>(executor, workingQueue, pendingQueue, parallelism);
        this.collectionFactory = collection;
        this.operation = operation;
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> acc.add(dispatcher.enqueue(() -> operation.apply(e)));
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        if (dispatcher.isNotEmpty()) {
            dispatcher.start();
            return foldLeftFutures(collectionFactory).andThen(f -> supplyWithResources(() -> f, dispatcher::close));
        } else {
            return supplyWithResources(() -> (__) -> completedFuture(collectionFactory
              .get()), dispatcher::close);
        }
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }

    @Override
    public void close() {
        dispatcher.close();
    }
}
