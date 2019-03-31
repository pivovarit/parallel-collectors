package com.pivovarit.collectors;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * @author Grzegorz Piwowarek
 */
abstract class AbstractAsyncUnorderedParallelCollector<T, R, C>
  extends AbstractAsyncCollector<T, R, C>
  implements AutoCloseable {

    private final Dispatcher<R> dispatcher;
    private final Function<T, R> function;

    AbstractAsyncUnorderedParallelCollector(
      Function<T, R> function,
      Executor executor,
      int parallelism) {
        this.dispatcher = new ThrottlingDispatcher<>(executor, parallelism);
        this.function = function;
    }

    AbstractAsyncUnorderedParallelCollector(
      Function<T, R> function,
      Executor executor) {
        this.dispatcher = new UnboundedDispatcher<>(executor);
        this.function = function;
    }

    abstract Function<CompletableFuture<Stream<R>>, CompletableFuture<C>> resultsProcessor();

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> acc.add(dispatcher.enqueue(() -> function.apply(e)));
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        if (!dispatcher.isEmpty()) {
            dispatcher.start();
            return futures -> resultsProcessor()
              .andThen(f -> supplyWithResources(() -> f, dispatcher::close))
              .apply(combineResults(futures));
        } else {
            return futures -> resultsProcessor().apply(completedFuture(Stream.empty()));
        }
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }

    @Override
    public void close() {
        dispatcher.close();
    }

    private static <T> CompletableFuture<Stream<T>> combineResults(List<CompletableFuture<T>> futures) {
        return allOf(futures.toArray(new CompletableFuture<?>[0]))
          .thenApply(__ -> futures.stream()
            .map(CompletableFuture::join));
    }
}
