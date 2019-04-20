package com.pivovarit.collectors;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Stream;

final class SyncOrderedStreamParallelCollector<T, R> extends AbstractSyncStreamCollector<T, R> {

    SyncOrderedStreamParallelCollector(
      Function<T, R> function,
      Executor executor,
      int parallelism) {
        super(function, executor, parallelism);
    }

    SyncOrderedStreamParallelCollector(
      Function<T, R> function,
      Executor executor) {
        super(function, executor);
    }

    @Override
    Function<List<CompletableFuture<R>>, Stream<R>> postProcess() {
        return futures -> futures.stream().map(CompletableFuture::join);
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
