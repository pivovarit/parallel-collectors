package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.BatchingSpliterator.partitioned;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.allOf;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncParallelCollector<T, R, C>
  implements Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>> {

    private final Dispatcher<R> dispatcher;
    private final Function<? super T, ? extends R> task;
    private final Function<Stream<R>, C> finalizer;

    private AsyncParallelCollector(
      Function<? super T, ? extends R> task,
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

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collecting(Function<? super T, ? extends R> mapper, Option... options) {
        Function<Stream<R>, Stream<R>> finalizer = i -> i;
        return collecting(finalizer, mapper, options);
    }

    static <T, R, C> Collector<T, ?, CompletableFuture<C>> collecting(Function<Stream<R>, C> finalizer, Function<? super T, ? extends R> mapper, Option... options) {
        requireNonNull(mapper, "mapper can't be null");

        var config = Option.process(options);

        var batching = config.batching().orElse(false);
        var executor = config.executor().orElseGet(Executors::newVirtualThreadPerTaskExecutor);

        if (config.parallelism().orElse(-1) == 1) {
            return new AsyncCollector<>(mapper, finalizer, executor);
        }

        if (batching) {
            var parallelism = config.parallelism().orElseThrow(() -> new IllegalArgumentException("it's obligatory to provide parallelism when using batching"));
            return new BatchingCollector<>(mapper, finalizer, executor, parallelism);
        }

        return config.parallelism().isPresent()
          ? new AsyncParallelCollector<>(mapper, Dispatcher.from(executor, config.parallelism().getAsInt()), finalizer)
          : new AsyncParallelCollector<>(mapper, Dispatcher.from(executor), finalizer);
    }

    private static class AsyncCollector<T, R, RR>
      implements Collector<T, Stream.Builder<T>, CompletableFuture<RR>> {

        private final Function<? super T, ? extends R> mapper;
        private final Function<Stream<R>, RR> finisher;

        private final Executor executor;

        AsyncCollector(Function<? super T, ? extends R> mapper, Function<Stream<R>, RR> finisher, Executor executor) {
            this.mapper = mapper;
            this.finisher = finisher;
            this.executor = executor;
        }

        @Override
        public Supplier<Stream.Builder<T>> supplier() {
            return Stream::builder;
        }

        @Override
        public BiConsumer<Stream.Builder<T>, T> accumulator() {
            return Stream.Builder::add;
        }

        @Override
        public BinaryOperator<Stream.Builder<T>> combiner() {
            return (left, right) -> {
                right.build().forEach(left::add);
                return left;
            };
        }

        @Override
        public Function<Stream.Builder<T>, CompletableFuture<RR>> finisher() {
            return acc -> {
                try {
                    return CompletableFuture.supplyAsync(() -> finisher.apply(acc.build().map(mapper)), executor);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }

    private record BatchingCollector<T, R, C>(Function<? super T, ? extends R> task, Function<Stream<R>, C> finalizer, Executor executor, int parallelism)
      implements Collector<T, ArrayList<T>, CompletableFuture<C>> {

        @Override
        public Supplier<ArrayList<T>> supplier() {
            return ArrayList::new;
        }

        @Override
        public BiConsumer<ArrayList<T>, T> accumulator() {
            return ArrayList::add;
        }

        @Override
        public BinaryOperator<ArrayList<T>> combiner() {
            return (left, right) -> {
                left.addAll(right);
                return left;
            };
        }

        @Override
        public Function<ArrayList<T>, CompletableFuture<C>> finisher() {
            return items -> {
                if (items.size() == parallelism) {
                    return items.stream()
                      .collect(new AsyncParallelCollector<>(task, Dispatcher.from(executor, parallelism), finalizer));
                } else {
                    return partitioned(items, parallelism)
                      .collect(new AsyncParallelCollector<>(batch -> {
                          List<R> list = new ArrayList<>(batch.size());
                          for (T t : batch) {
                              list.add(task.apply(t));
                          }
                          return list;
                      }, Dispatcher.from(executor, parallelism), r -> finalizer.apply(r.flatMap(Collection::stream))));
                }
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.emptySet();
        }
    }
}
