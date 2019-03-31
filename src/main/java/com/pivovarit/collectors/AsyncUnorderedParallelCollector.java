package com.pivovarit.collectors;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncUnorderedParallelCollector<T, R, C extends Collection<R>>
  extends AbstractAsyncUnorderedParallelCollector<T, R, C>
  implements AutoCloseable {

    private final Supplier<C> collectionFactory;

    AsyncUnorderedParallelCollector(
      Function<T, R> function,
      Supplier<C> collection,
      Executor executor,
      int parallelism) {
        super(function, executor, parallelism);
        this.collectionFactory = collection;
    }

    AsyncUnorderedParallelCollector(
      Function<T, R> function,
      Supplier<C> collection,
      Executor executor) {
        super(function, executor);
        this.collectionFactory = collection;
    }

    @Override
    Function<CompletableFuture<Stream<R>>, CompletableFuture<C>> resultsProcessor() {
        return result -> result.thenApply(futures -> futures.collect(Collectors.toCollection(collectionFactory)));
    }
}
