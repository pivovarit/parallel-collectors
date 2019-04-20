package com.pivovarit.collectors;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class SyncCompletionOrderParallelCollector<T, R> extends AbstractSyncStreamCollector<T, R> {

    SyncCompletionOrderParallelCollector(
      Function<T, R> function,
      Executor executor,
      int parallelism) {
        super(function, executor, parallelism);
    }

    SyncCompletionOrderParallelCollector(
      Function<T, R> function,
      Executor executor) {
        super(function, executor);
    }

    @Override
    Function<List<CompletableFuture<R>>, Stream<R>> postProcess() {
        return futures -> StreamSupport.stream(new CompletionOrderSpliterator<>(futures), false);
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }
}
