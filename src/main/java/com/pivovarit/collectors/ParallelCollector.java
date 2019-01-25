package com.pivovarit.collectors;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author Grzegorz Piwowarek
 */
class ParallelCollector<T, R1, R2 extends Collection<R1>> extends AbstractParallelCollector<T, R1, R2>
  implements Collector<T, List<CompletableFuture<R1>>, CompletableFuture<R2>> {
    ParallelCollector(
      Function<T, R1> operation,
      Supplier<R2> collection,
      Executor executor) {
        super(operation, collection, executor);
    }
}
