package com.pivovarit.collectors;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncParallelCollector<T, R, C extends Collection<R>>
  extends AbstractAsyncParallelCollector<T, R, C> {

    private final Supplier<C> collectionFactory;

    AsyncParallelCollector(
      Function<T, R> function,
      Supplier<C> collection,
      Executor executor,
      int parallelism) {
        super(function, executor, parallelism);
        this.collectionFactory = collection;
    }

    AsyncParallelCollector(
      Function<T, R> function,
      Supplier<C> collection,
      Executor executor) {
        super(function, executor);
        this.collectionFactory = collection;
    }

    @Override
    Function<CompletableFuture<Stream<R>>, CompletableFuture<C>> postProcess() {
        return result -> result.thenApply(futures -> futures.collect(toCollection(collectionFactory)));
    }
}
