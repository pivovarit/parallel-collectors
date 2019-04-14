package com.pivovarit.collectors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncOrderedParallelStreamCollector<T, R>
  extends AbstractAsyncOrderedParallelCollector<T, R, Stream<R>> {

    AsyncOrderedParallelStreamCollector(
      Function<T, R> function,
      Executor executor,
      int parallelism) {
        super(function, executor, parallelism);
    }

    AsyncOrderedParallelStreamCollector(
      Function<T, R> function,
      Executor executor) {
        super(function, executor);
    }

    @Override
    Function<CompletableFuture<Stream<R>>, CompletableFuture<Stream<R>>> resultsProcessor() {
        return result -> result;
    }
}
