package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
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

import static com.pivovarit.collectors.BatchingSpliterator.batching;
import static com.pivovarit.collectors.BatchingSpliterator.partitioned;
import static com.pivovarit.collectors.CompletionStrategy.ordered;
import static com.pivovarit.collectors.CompletionStrategy.unordered;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

/**
 * @author Grzegorz Piwowarek
 */
class AsyncParallelStreamingCollector<T, R> implements Collector<T, List<CompletableFuture<R>>, Stream<R>> {

    private final Function<? super T, ? extends R> function;

    private final CompletionStrategy<R> completionStrategy;

    private final Set<Characteristics> characteristics;

    private final Dispatcher<R> dispatcher;

    private AsyncParallelStreamingCollector(
      Function<? super T, ? extends R> function,
      Dispatcher<R> dispatcher,
      boolean ordered) {
        this.completionStrategy = ordered ? ordered() : unordered();
        this.characteristics = switch (completionStrategy) {
            case CompletionStrategy.Ordered<R> __ -> emptySet();
            case CompletionStrategy.Unordered<R> __ -> EnumSet.of(Characteristics.UNORDERED);
        };
        this.dispatcher = dispatcher;
        this.function = function;
    }

    @Override
    public Supplier<List<CompletableFuture<R>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> {
            dispatcher.start();
            acc.add(dispatcher.enqueue(() -> function.apply(e)));
        };
    }

    @Override
    public BinaryOperator<List<CompletableFuture<R>>> combiner() {
        return (left, right) -> {
            throw new UnsupportedOperationException(
              "Using parallel stream with parallel collectors is a bad idea");
        };
    }

    @Override
    public Function<List<CompletableFuture<R>>, Stream<R>> finisher() {
        return acc -> {
            dispatcher.stop();
            return completionStrategy.apply(acc);
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return characteristics;
    }

    static <T, R> Collector<T, ?, Stream<R>> streaming(Function<? super T, ? extends R> mapper, boolean ordered, Option... options) {
        requireNonNull(mapper, "mapper can't be null");

        var config = Option.process(options);

        if (config.batching().isPresent() && config.batching().get()) {
            if (config.parallelism().isEmpty()) {
                throw new IllegalArgumentException("it's obligatory to provide parallelism when using batching");
            }

            var parallelism = config.parallelism().orElseThrow();

            if (config.executor().isPresent()) {
                var executor = config.executor().orElseThrow();

                return parallelism == 1
                  ? new SynchronousCollector<>(mapper)
                  : new BatchingCollector<>(mapper, executor, parallelism, ordered);
            } else {
                return new BatchingCollector<>(mapper, parallelism, ordered);
            }
        } else {
            if (config.executor().isPresent() && config.parallelism().isPresent()) {
                var executor = config.executor().orElseThrow();
                var parallelism = config.parallelism().orElseThrow();

                return new AsyncParallelStreamingCollector<>(mapper, Dispatcher.from(executor, parallelism), ordered);
            } else if (config.executor().isPresent()) {
                var executor = config.executor().orElseThrow();

                return new AsyncParallelStreamingCollector<>(mapper, Dispatcher.from(executor), ordered);
            } else if (config.parallelism().isPresent()) {
                var parallelism = config.parallelism().orElseThrow();

                return new AsyncParallelStreamingCollector<>(mapper, Dispatcher.virtual(parallelism), ordered);
            }

            return new AsyncParallelStreamingCollector<>(mapper, Dispatcher.virtual(), ordered);
        }
    }

    private static final class SynchronousCollector<T, R> implements Collector<T, Stream.Builder<R>, Stream<R>> {

        private final Function<? super T, ? extends R> mapper;

        SynchronousCollector(Function<? super T, ? extends R> mapper) {
            this.mapper = mapper;
        }

        @Override
        public Supplier<Stream.Builder<R>> supplier() {
            return Stream::builder;
        }

        @Override
        public BiConsumer<Stream.Builder<R>, T> accumulator() {
            return (rs, t) -> rs.add(mapper.apply(t));
        }

        @Override
        public BinaryOperator<Stream.Builder<R>> combiner() {
            return (rs, rs2) -> {
                throw new UnsupportedOperationException("Using parallel stream with parallel collectors is a bad idea");
            };
        }

        @Override
        public Function<Stream.Builder<R>, Stream<R>> finisher() {
            return Stream.Builder::build;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }

    private static final class BatchingCollector<T, R>
      implements Collector<T, ArrayList<T>, Stream<R>> {

        private final Function<? super T, ? extends R> task;
        private final Executor executor;
        private final int parallelism;
        private final boolean ordered;

        BatchingCollector(
          Function<? super T, ? extends R> task,
          Executor executor,
          int parallelism,
          boolean ordered) {
            this.executor = executor;
            this.task = task;
            this.parallelism = parallelism;
            this.ordered = ordered;
        }

        BatchingCollector(
          Function<? super T, ? extends R> task,
          int parallelism,
          boolean ordered) {
            this.executor = null;
            this.task = task;
            this.parallelism = parallelism;
            this.ordered = ordered;
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
        public Function<ArrayList<T>, Stream<R>> finisher() {
            return items -> {
                if (items.size() == parallelism) {
                    return items.stream()
                      .collect(new AsyncParallelStreamingCollector<>(task, resolveDispatcher(), ordered));
                } else {
                    return partitioned(items, parallelism)
                      .collect(new AsyncParallelStreamingCollector<>(batching(task), resolveDispatcher(), ordered))
                      .flatMap(Collection::stream);
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
            return emptySet();
        }
    }
}
