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
final class AsyncParallelCollectionCollector<T, R, C extends Collection<R>>
  extends AsyncParallelCollector<T, R, C> {

    AsyncParallelCollectionCollector(
      Function<T, R> function,
      Supplier<C> collection,
      Executor executor,
      int parallelism) {
        super(function, postProcess(collection), executor, parallelism);
    }

    AsyncParallelCollectionCollector(
      Function<T, R> function,
      Supplier<C> collection,
      Executor executor) {
        super(function, postProcess(collection), executor);
    }

    private static <R, C extends Collection<R>> Function<CompletableFuture<Stream<R>>, CompletableFuture<C>> postProcess(Supplier<C> collectionFactory) {
        return result -> result.thenApply(futures -> futures.collect(toCollection(collectionFactory)));
    }
}
