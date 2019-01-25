package com.pivovarit.collectors;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * @author Grzegorz Piwowarek
 */
class ThrottledParallelCollector<T, R1, R2 extends Collection<R1>> extends AbstractParallelCollector<T, R1, R2>
  implements Collector<T, List<CompletableFuture<R1>>, CompletableFuture<R2>> {

    private final Semaphore permits;

    ThrottledParallelCollector(
      Function<T, R1> operation,
      Supplier<R2> collection,
      Executor executor,
      int parallelism) {
        super(operation, collection, executor);
        this.permits = new Semaphore(parallelism);
    }

    @Override
    public BiConsumer<List<CompletableFuture<R1>>, T> accumulator() {
        return (acc, e) -> {
            try {
                permits.acquire();
                acc.add(supplyAsync(() -> {
                    try {
                        return operation.apply(e);
                    } finally {
                        permits.release();
                    }
                }, executor));
            } catch (InterruptedException e1) {
                permits.release();
                Thread.currentThread().interrupt();
            } catch (RejectedExecutionException ex) {
                permits.release();
                throw ex;
            }
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }
}
