package com.pivovarit.collectors;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * @author Grzegorz Piwowarek
 */
final class BlockingParallelCollector<T, R, C extends Collection<R>>
  extends AbstractParallelCollector<T, R, C>
  implements AutoCloseable {

    private final Dispatcher<R> dispatcher;

    private final Function<T, R> operation;
    private final Supplier<C> collectionFactory;

    BlockingParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor,
      int parallelism) {
        this.dispatcher = new ThrottlingDispatcher<>(executor, parallelism);
        this.collectionFactory = collection;
        this.operation = operation;
    }

    BlockingParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor) {
        this.dispatcher = new UnboundedDispatcher<>(executor);
        this.collectionFactory = collection;
        this.operation = operation;
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> acc.add(dispatcher.enqueue(() -> operation.apply(e)));
    }

    @Override
    public Function<List<CompletableFuture<R>>, C> finisher() {
        if (dispatcher.getWorkingQueue().size() != 0) {
            dispatcher.start();
            return foldLeftFutures(collectionFactory)
              .andThen(future -> supplyWithResources(future::join, dispatcher::close));
        } else {
            return supplyWithResources(() -> (__) -> collectionFactory.get(), dispatcher::close);
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
