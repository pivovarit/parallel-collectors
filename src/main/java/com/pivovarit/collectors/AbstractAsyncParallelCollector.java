package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * @author Grzegorz Piwowarek
 */
abstract class AbstractAsyncParallelCollector<T, R, C>
  implements Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>> {

    private final Dispatcher<R> dispatcher;
    private final Function<T, R> function;

    protected final CompletableFuture<C> result = new CompletableFuture<>();

    AbstractAsyncParallelCollector(
      Function<T, R> function,
      Executor executor,
      int parallelism) {
        this.dispatcher = new Dispatcher<>(executor, parallelism);
        this.function = function;
    }

    AbstractAsyncParallelCollector(
      Function<T, R> function,
      Executor executor) {
        this.dispatcher = new Dispatcher<>(executor);
        this.function = function;
    }

    abstract Function<CompletableFuture<Stream<R>>, CompletableFuture<C>> postProcess();


    @Override
    public Supplier<List<CompletableFuture<R>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BinaryOperator<List<CompletableFuture<R>>> combiner() {
        return (left, right) -> {
            left.addAll(right);
            return left;
        };
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> acc.add(dispatcher.enqueue(() -> function.apply(e)));
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        if (!dispatcher.isEmpty()) {
            dispatcher.start()
              .whenComplete((aVoid, throwable) -> {
                  if (throwable != null) {
                      result.completeExceptionally(throwable);
                  }
              });
            return futures -> {
                postProcess()
                  .apply(combineResults(futures))
                  .whenComplete((c, throwable) -> {
                      if (throwable == null) {
                          result.complete(c);
                      } else {
                          result.completeExceptionally(throwable);
                      }
                  });

                return result;
            };
        } else {
            return futures -> postProcess().apply(completedFuture(Stream.empty()));
        }
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }

    private static <T> CompletableFuture<Stream<T>> combineResults(List<CompletableFuture<T>> futures) {
        return allOf(futures.toArray(new CompletableFuture<?>[0]))
          .thenApply(__ -> futures.stream()
            .map(CompletableFuture::join));
    }
}
