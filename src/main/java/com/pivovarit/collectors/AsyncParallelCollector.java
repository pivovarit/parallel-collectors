package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import static com.pivovarit.collectors.BatchingSpliterator.batching;
import static com.pivovarit.collectors.BatchingSpliterator.partitioned;
import static com.pivovarit.collectors.Dispatcher.getDefaultParallelism;
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
    private final Function<T, R> mapper;
    private final Function<Stream<R>, C> processor;

    private AsyncParallelCollector(
      Function<T, R> mapper,
      Dispatcher<R> dispatcher,
      Function<Stream<R>, C> processor) {
        this.dispatcher = dispatcher;
        this.processor = processor;
        this.mapper = mapper;
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
            acc.add(dispatcher.enqueue(() -> mapper.apply(e)));
        };
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        return futures -> {
            dispatcher.stop();

            return combine(futures).thenApply(processor);
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }

    private static <T> CompletableFuture<Stream<T>> combine(List<CompletableFuture<T>> futures) {
        CompletableFuture<T>[] futuresArray = (CompletableFuture<T>[]) futures.toArray(new CompletableFuture[0]);
        CompletableFuture<Stream<T>> combined = allOf(futuresArray)
          .thenApply(__ -> Arrays.stream(futuresArray).map(CompletableFuture::join));

        for (CompletableFuture<?> f : futuresArray) {
            f.exceptionally(ex -> {
                combined.completeExceptionally(ex);
                return null;
            });
        }

        return combined;
    }

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collectingToStream(Function<T, R> mapper, Executor executor) {
        return collectingToStream(mapper, executor, getDefaultParallelism());
    }

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collectingToStream(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);

        return parallelism == 1
          ? asyncCollector(mapper, executor, i -> i)
          : new AsyncParallelCollector<>(mapper, Dispatcher.of(executor, parallelism), t -> t);
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> collectingWithCollector(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor) {
        return collectingWithCollector(collector, mapper, executor, getDefaultParallelism());
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> collectingWithCollector(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(collector, "collector can't be null");
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);

        return parallelism == 1
          ? asyncCollector(mapper, executor, s -> s.collect(collector))
          : new AsyncParallelCollector<>(mapper, Dispatcher.of(executor, parallelism), s -> s.collect(collector));
    }

    static void requireValidParallelism(int parallelism) {
        if (parallelism < 1) {
            throw new IllegalArgumentException("Parallelism can't be lower than 1");
        }
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> asyncCollector(Function<T, R> mapper, Executor executor, Function<Stream<R>, RR> finisher) {
        return collectingAndThen(toList(), list -> supplyAsync(() -> {
            Stream.Builder<R> acc = Stream.builder();
            for (T t : list) {
                acc.add(mapper.apply(t));
            }
            return finisher.apply(acc.build());
        }, executor));
    }

    static final class BatchingCollectors {

        private BatchingCollectors() {
        }

        static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> collectingWithCollector(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor, int parallelism) {
            requireNonNull(collector, "collector can't be null");
            requireNonNull(executor, "executor can't be null");
            requireNonNull(mapper, "mapper can't be null");
            requireValidParallelism(parallelism);

            return parallelism == 1
              ? asyncCollector(mapper, executor, s -> s.collect(collector))
              : batchingCollector(mapper, executor, parallelism, s -> s.collect(collector));
        }

        static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collectingToStream(
          Function<T, R> mapper,
          Executor executor, int parallelism) {
            requireNonNull(executor, "executor can't be null");
            requireNonNull(mapper, "mapper can't be null");
            requireValidParallelism(parallelism);

            return parallelism == 1
              ? asyncCollector(mapper, executor, i -> i)
              : batchingCollector(mapper, executor, parallelism, s -> s);
        }

        private static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> batchingCollector(Function<T, R> mapper, Executor executor, int parallelism, Function<Stream<R>, RR> finisher) {
            return collectingAndThen(
              toList(),
              list -> {
                  // no sense to repack into batches of size 1
                  if (list.size() == parallelism) {
                      return list.stream()
                        .collect(new AsyncParallelCollector<>(
                          mapper,
                          Dispatcher.of(executor, parallelism),
                          finisher));
                  } else {
                      return partitioned(list, parallelism)
                        .collect(new AsyncParallelCollector<>(
                          batching(mapper),
                          Dispatcher.of(executor, parallelism),
                          listStream -> finisher.apply(listStream.flatMap(Collection::stream))));
                  }
              });
        }
    }
}
