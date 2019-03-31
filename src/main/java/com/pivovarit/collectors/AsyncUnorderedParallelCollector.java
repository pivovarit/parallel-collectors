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
final class AsyncUnorderedParallelCollector<T, R, C extends Collection<R>>
  extends AbstractAsyncCollector<T, R, C>
  implements AutoCloseable {

    private final Dispatcher<R> dispatcher;

    private final Function<T, R> function;
    private final Supplier<C> collectionFactory;

    AsyncUnorderedParallelCollector(
      Function<T, R> function,
      Supplier<C> collection,
      Executor executor,
      int parallelism) {
        this.dispatcher = new ThrottlingDispatcher<>(executor, parallelism);
        this.collectionFactory = collection;
        this.function = function;
    }

    AsyncUnorderedParallelCollector(
      Function<T, R> function,
      Supplier<C> collection,
      Executor executor) {
        this.dispatcher = new UnboundedDispatcher<>(executor);
        this.collectionFactory = collection;
        this.function = function;
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> acc.add(dispatcher.enqueue(() -> function.apply(e)));
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        if (!dispatcher.isEmpty()) {
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
