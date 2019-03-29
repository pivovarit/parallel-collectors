package com.pivovarit.collectors;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.concurrent.CompletableFuture.allOf;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncCustomizableParallelCollector<T, R, C>
  extends AbstractAsyncCollector<T, R, C>
  implements AutoCloseable {

    private final Dispatcher<R> dispatcher;

    private final Function<T, R> function;

    private final Collector<R, ?, C> collector;

    AsyncCustomizableParallelCollector(
      Function<T, R> function,
      Collector<R, ?, C> collector,
      Executor executor,
      int parallelism) {
        this.dispatcher = new ThrottlingDispatcher<>(executor, parallelism);
        this.collector = collector;
        this.function = function;
    }

    AsyncCustomizableParallelCollector(
      Function<T, R> function,
      Collector<R, ?, C> collector,
      Executor executor) {
        this.dispatcher = new UnboundedDispatcher<>(executor);
        this.collector = collector;
        this.function = function;
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> acc.add(dispatcher.enqueue(() -> function.apply(e)));
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        if (!dispatcher.isEmpty()) {
            dispatcher.start();
        }

        return collectingWith(collector).andThen(f -> supplyWithResources(() -> f, dispatcher::close));
    }

    private Function<List<CompletableFuture<R>>, CompletableFuture<C>> collectingWith(Collector<R, ?, C> collector) {
        return futures -> allOf(futures.toArray(new CompletableFuture<?>[0]))
          .thenApply(__ -> futures.stream()
            .map(CompletableFuture::join)
            .collect(collector));
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }

    @Override
    public void close() {
        dispatcher.close();
    }
}
