package com.pivovarit.collectors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncParallelStreamCollector<T, R>
  extends AbstractAsyncParallelCollector<T, R, Stream<R>> {

    AsyncParallelStreamCollector(
      Function<T, R> function,
      Executor executor,
      int parallelism) {
        super(function, executor, parallelism);
    }

    AsyncParallelStreamCollector(
      Function<T, R> function,
      Executor executor) {
        super(function, executor);
    }

    @Override
    Function<CompletableFuture<Stream<R>>, CompletableFuture<Stream<R>>> postProcess() {
        return result -> result;
    }
}
