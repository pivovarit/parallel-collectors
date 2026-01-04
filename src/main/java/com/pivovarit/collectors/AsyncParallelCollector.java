package com.pivovarit.collectors;

import java.util.ArrayList;
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

import static com.pivovarit.collectors.BatchingSpliterator.partitioned;
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

    public static <T,R, C> Collector<T, ?, CompletableFuture<C>> from(
      Function<? super T, ? extends R> task,
      Function<Stream<R>, C> finalizer,
      Executor executor,
      int parallelism) {
        return new AsyncParallelCollector<>(task, new Dispatcher<>(executor, parallelism), finalizer);
    }

    public static <T,R, C> Collector<T, ?, CompletableFuture<C>> from(
      Function<? super T, ? extends R> task,
      Function<Stream<R>, C> finalizer,
      Executor executor) {
        return new AsyncParallelCollector<>(task, new Dispatcher<>(executor), finalizer);
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
            acc.add(dispatcher.submit(() -> task.apply(e)));
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

    record BatchingCollector<T, R, C>(Function<? super T, ? extends R> task, Function<Stream<R>, C> finalizer, Executor executor, int parallelism)
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
                      .collect(AsyncParallelCollector.from(task, finalizer, executor, parallelism));
                } else {
                    return partitioned(items, parallelism)
                      .collect(AsyncParallelCollector.from(batch -> {
                          List<R> list = new ArrayList<>(batch.size());
                          for (T t : batch) {
                              list.add(task.apply(t));
                          }
                          return list;
                      }, r -> finalizer.apply(r.flatMap(Collection::stream)), executor, parallelism));
                }
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.emptySet();
        }
    }
}
