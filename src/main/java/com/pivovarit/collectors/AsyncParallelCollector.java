package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.BatchingSpliterator.batching;
import static com.pivovarit.collectors.BatchingSpliterator.partitioned;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncParallelCollector<T, R, C>
  implements Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>> {

    private final Dispatcher<R> dispatcher;
    private final Function<? super T, R> task;
    private final Function<Stream<R>, C> finalizer;

    private AsyncParallelCollector(
      Function<? super T, R> task,
      Dispatcher<R> dispatcher,
      Function<Stream<R>, C> finalizer) {
        this.dispatcher = dispatcher;
        this.finalizer = finalizer;
        this.task = task;
    }

    @Override
    public Supplier<List<CompletableFuture<R>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BinaryOperator<List<CompletableFuture<R>>> combiner() {
        return (left, right) -> {
            throw new UnsupportedOperationException("Using parallel stream with parallel collectors is a bad idea");
        };
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> {
            if (!dispatcher.isRunning()) {
                dispatcher.start();
            }
            acc.add(dispatcher.enqueue(() -> task.apply(e)));
        };
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        return futures -> {
            dispatcher.stop();

            return combine(futures).thenApply(finalizer);
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }

    private static <T> CompletableFuture<Stream<T>> combine(List<CompletableFuture<T>> futures) {
        var combined = allOf(futures.toArray(CompletableFuture[]::new))
          .thenApply(__ -> futures.stream().map(CompletableFuture::join));

        for (var future : futures) {
            future.whenComplete((o, ex) -> {
                if (ex != null) {
                    combined.completeExceptionally(ex);
                }
            });
        }

        return combined;
    }

    static <T, R, C> Collector<T, ?, CompletableFuture<C>> collecting(Function<Stream<R>, C> finalizer, Function<? super T, R> mapper, Option... options) {
        requireNonNull(mapper, "mapper can't be null");

        Option.Configuration config = Option.process(options);

        if (config.batching().isPresent() && config.batching().get()) {
            if (config.parallelism().isEmpty()) {
                throw new IllegalArgumentException("it's obligatory to provide parallelism when using batching");
            }

            var parallelism = config.parallelism().orElseThrow();

            if (config.executor().isPresent()) {
                var executor = config.executor().orElseThrow();
                return parallelism == 1
                  ? asyncCollector(mapper, executor, finalizer)
                  : batchingCollector(mapper, executor, parallelism, finalizer);
            } else {
                return batchingCollector(mapper, parallelism, finalizer);
            }
        }

        if (config.executor().isPresent() && config.parallelism().isPresent()) {
            var executor = config.executor().orElseThrow();
            var parallelism = config.parallelism().orElseThrow();

            return parallelism == 1
              ? asyncCollector(mapper, executor, finalizer)
              : new AsyncParallelCollector<>(mapper, Dispatcher.from(executor, parallelism), finalizer);
        } else if (config.executor().isPresent()) {
            var executor = config.executor().orElseThrow();

            return new AsyncParallelCollector<>(mapper, Dispatcher.from(executor), finalizer);
        } else if (config.parallelism().isPresent()) {
            var parallelism = config.parallelism().orElseThrow();

            return new AsyncParallelCollector<>(mapper, Dispatcher.virtual(parallelism), finalizer);
        } else {
            return new AsyncParallelCollector<>(mapper, Dispatcher.virtual(), finalizer);
        }
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> asyncCollector(Function<? super T, R> mapper, Executor executor, Function<Stream<R>, RR> finisher) {
        return collectingAndThen(toList(), list -> {
            try {
                return supplyAsync(() -> {
                    Stream.Builder<R> acc = Stream.builder();
                    for (T t : list) {
                        acc.add(mapper.apply(t));
                    }
                    return finisher.apply(acc.build());
                }, executor);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    private static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> batchingCollector(Function<? super T, R> mapper, int parallelism, Function<Stream<R>, RR> finisher) {
        return batchingCollector(mapper, null, parallelism, finisher);
    }

    private static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> batchingCollector(Function<? super T, R> mapper, Executor executor, int parallelism, Function<Stream<R>, RR> finisher) {
        return collectingAndThen(
          toList(),
          list -> {
              // no sense to repack into batches of size 1
              if (list.size() == parallelism) {
                  return list.stream()
                    .collect(new AsyncParallelCollector<>(
                      mapper,
                      executor != null
                        ? Dispatcher.from(executor, parallelism)
                        : Dispatcher.virtual(parallelism),
                      finisher));
              } else {
                  return partitioned(list, parallelism)
                    .collect(new AsyncParallelCollector<>(
                      batching(mapper),
                      executor != null
                        ? Dispatcher.from(executor, parallelism)
                        : Dispatcher.virtual(parallelism),
                      listStream -> finisher.apply(listStream.flatMap(Collection::stream))));
              }
          });
    }
}
