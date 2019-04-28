package com.pivovarit.collectors;

import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.function.Function.identity;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncParallelStreamCollector<T, R>
  extends AsyncParallelCollector<T, R, Stream<R>> {

    AsyncParallelStreamCollector(
      Function<T, R> function,
      Executor executor,
      int parallelism) {
        super(function, identity(), executor, parallelism);
    }

    AsyncParallelStreamCollector(
      Function<T, R> function,
      Executor executor) {
        super(function, identity(), executor);
    }
}
