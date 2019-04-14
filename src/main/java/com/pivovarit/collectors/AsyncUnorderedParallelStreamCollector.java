package com.pivovarit.collectors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncUnorderedParallelStreamCollector<T, R>
  extends AbstractAsyncUnorderedParallelCollector<T, R, Stream<R>> {

    AsyncUnorderedParallelStreamCollector(
      Function<T, R> function,
      Executor executor,
      int parallelism) {
        super(function, executor, parallelism);
    }

    AsyncUnorderedParallelStreamCollector(
      Function<T, R> function,
      Executor executor) {
        super(function, executor);
    }

    @Override
    Function<CompletableFuture<Stream<R>>, CompletableFuture<Stream<R>>> resultsProcessor() {
        return result -> result;
    }
}
