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

        Option.Configuration config = Option.process(options);

        if (config.batching().isPresent() && config.batching().get()) {
            if (config.parallelism().isEmpty()) {
                throw new IllegalArgumentException("it's obligatory to provide parallelism when using batching");
            }

            var parallelism = config.parallelism().orElseThrow();

            if (config.executor().isPresent()) {
                var executor = config.executor().orElseThrow();
                return parallelism == 1
                  ? new AsyncCollector<>(mapper, finalizer, executor)
                  : new BatchingCollector<>(mapper, finalizer, executor, parallelism);
            } else {
                return new BatchingCollector<>(mapper, finalizer, parallelism);
            }
        }

        if (config.executor().isPresent() && config.parallelism().isPresent()) {
            var executor = config.executor().orElseThrow();
            var parallelism = config.parallelism().orElseThrow();

            return parallelism == 1
              ? new AsyncCollector<>(mapper, finalizer, executor)
              : new AsyncParallelCollector<>(mapper, Dispatcher.from(executor, parallelism), finalizer);
        } else if (config.executor().isPresent()) {
            var executor = config.executor().orElseThrow();

            return new AsyncParallelCollector<>(mapper, Dispatcher.from(executor), finalizer);
        } else if (config.parallelism().isPresent()) {
            var parallelism = config.parallelism().orElseThrow();

            return parallelism == 1
              ? new AsyncCollector<>(mapper, finalizer, Executors.newVirtualThreadPerTaskExecutor())
              : new AsyncParallelCollector<>(mapper, Dispatcher.virtual(parallelism), finalizer);
        } else {
            return new AsyncParallelCollector<>(mapper, Dispatcher.virtual(), finalizer);
        }
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

    private static final class BatchingCollector<T, R, C>
      implements Collector<T, ArrayList<T>, CompletableFuture<C>> {

        private final Function<? super T, ? extends R> task;
        private final Function<Stream<R>, C> finalizer;
        private final Executor executor;
        private final int parallelism;

        BatchingCollector(
          Function<? super T, ? extends R> task,
          Function<Stream<R>, C> finalizer,
          Executor executor,
          int parallelism) {
            this.executor = executor;
            this.finalizer = finalizer;
            this.task = task;
            this.parallelism = parallelism;
        }

        BatchingCollector(
          Function<? super T, ? extends R> task,
          Function<Stream<R>, C> finalizer,
          int parallelism) {
            this.executor = null;
            this.finalizer = finalizer;
            this.task = task;
            this.parallelism = parallelism;
        }

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
                      .collect(new AsyncParallelCollector<>(task, resolveDispatcher(), finalizer));
                } else {
                    return partitioned(items, parallelism)
                      .collect(new AsyncParallelCollector<>(batch -> {
                          List<R> list = new ArrayList<>(batch.size());
                          for (T t : batch) {
                              list.add(task.apply(t));
                          }
                          return list;
                      }, resolveDispatcher(), r -> finalizer.apply(r.flatMap(Collection::stream))));
                }
            };
        }

        private <TT> Dispatcher<TT> resolveDispatcher() {
            return executor != null
              ? Dispatcher.from(executor, parallelism)
              : Dispatcher.virtual(parallelism);
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.emptySet();
        }
    }
}
