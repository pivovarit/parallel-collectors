package com.pivovarit.collectors;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.stream.Collectors.toCollection;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncOrderedParallelCollector<T, R, C extends Collection<R>>
  extends AbstractAsyncOrderedParallelCollector<T, R, C> {

    private final Supplier<C> collectionFactory;

    AsyncOrderedParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor,
      int parallelism) {
        super(operation, executor, parallelism);
        this.collectionFactory = collection;
    }

    AsyncOrderedParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor) {
        super(operation, executor);
        this.collectionFactory = collection;
    }

    @Override
    Function<CompletableFuture<Stream<R>>, CompletableFuture<C>> resultsProcessor() {
        return result -> result.thenApply(futures -> futures.collect(Collectors.toCollection(collectionFactory)));
    }
}
