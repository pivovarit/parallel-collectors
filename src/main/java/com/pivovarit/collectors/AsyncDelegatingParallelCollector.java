package com.pivovarit.collectors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncDelegatingParallelCollector<T, R, C>
  extends AbstractAsyncUnorderedParallelCollector<T, R, C>
  implements AutoCloseable {

    private final Collector<R, ?, C> collector;

    AsyncDelegatingParallelCollector(
      Function<T, R> function,
      Collector<R, ?, C> collector,
      Executor executor,
      int parallelism) {
        super(function, executor, parallelism);
        this.collector = collector;
    }

    AsyncDelegatingParallelCollector(
      Function<T, R> function,
      Collector<R, ?, C> collector,
      Executor executor) {
        super(function, executor);
        this.collector = collector;
    }

    @Override
    Function<CompletableFuture<Stream<R>>, CompletableFuture<C>> resultsProcessor() {
        return result -> result.thenApply(futures -> futures.collect(collector));
    }
}
