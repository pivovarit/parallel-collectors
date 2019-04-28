package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collections;
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

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Function.identity;

/**
 * @author Grzegorz Piwowarek
 */
class AsyncParallelCollector<T, R, C>
  implements Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>> {

    private final Dispatcher<R> dispatcher;
    private final Function<T, R> function;
    private final Function<CompletableFuture<Stream<R>>, CompletableFuture<C>> processor;

    protected final CompletableFuture<C> result = new CompletableFuture<>();

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallelToStream(Function<T, R> mapper, Executor executor) {
        return parallelToStream(mapper, executor);
    }

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallelToStream(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);
        return new AsyncParallelCollector<>(mapper, identity(), executor, parallelism);
    }

    AsyncParallelCollector(
      Function<T, R> function,
      Function<CompletableFuture<Stream<R>>, CompletableFuture<C>> processor,
      Executor executor,
      int parallelism) {
        this.dispatcher = new Dispatcher<>(executor, parallelism);
        this.processor = processor;
        this.function = function;
    }

    AsyncParallelCollector(
      Function<T, R> function,
      Function<CompletableFuture<Stream<R>>, CompletableFuture<C>> processor,
      Executor executor) {
        this.dispatcher = new Dispatcher<>(executor);
        this.processor = processor;
        this.function = function;
    }

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
        return (acc, e) -> {
            startConsuming();
            acc.add(dispatcher.enqueue(() -> function.apply(e)));
        };
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        return futures -> {
            dispatcher.stop();

            processor
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
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }

    private static <T> CompletableFuture<Stream<T>> combineResults(List<CompletableFuture<T>> futures) {
        return allOf(futures.toArray(new CompletableFuture<?>[0]))
          .thenApply(__ -> futures.stream()
            .map(CompletableFuture::join));
    }

    private void startConsuming() {
        if (!dispatcher.isRunning()) {
            dispatcher.start()
              .whenComplete((__, throwable) -> {
                  if (throwable != null) {
                      result.completeExceptionally(throwable);
                  }
              });
        }
    }

    private static void requireValidParallelism(int parallelism) {
        if (parallelism < 1) {
            throw new IllegalArgumentException("Parallelism can't be lower than 1");
        }
    }
}
