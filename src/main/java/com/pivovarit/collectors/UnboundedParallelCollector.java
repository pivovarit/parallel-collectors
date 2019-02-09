package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static com.pivovarit.collectors.CollectorUtils.accumulatingResults;
import static com.pivovarit.collectors.CollectorUtils.mergingPartialResults;
import static com.pivovarit.collectors.CollectorUtils.supplyWithResources;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * @author Grzegorz Piwowarek
 */
class UnboundedParallelCollector<T, R, C extends Collection<R>>
  implements Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>>, AutoCloseable {

    volatile boolean isFailed = false;

    final ExecutorService dispatcher = newSingleThreadExecutor(new CustomThreadFactory());
    final Executor executor;
    final Queue<Supplier<R>> workingQueue;
    final Queue<CompletableFuture<R>> pending;
    final Supplier<C> collectionFactory;

    private final Function<T, R> operation;

    UnboundedParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor) {
        this(operation, collection, executor, new ConcurrentLinkedQueue<>(), new ConcurrentLinkedQueue<>());
    }

    UnboundedParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor,
      Queue<Supplier<R>> workingQueue,
      Queue<CompletableFuture<R>> pending) {
        this.executor = executor;
        this.collectionFactory = collection;
        this.operation = operation;
        this.workingQueue = workingQueue;
        this.pending = pending;
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
            CompletableFuture<R> future = new CompletableFuture<>();
            pending.add(future);
            workingQueue.add(() -> isFailed ? null : operation.apply(e));
            acc.add(future);
        };
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        if (workingQueue.size() != 0) {
            dispatcher.execute(dispatch(workingQueue));
            return foldLeftFutures().andThen(f -> supplyWithResources(() -> f, dispatcher::shutdown));
        } else {
            return supplyWithResources(() -> (__) -> completedFuture(collectionFactory.get()), dispatcher::shutdown);
        }
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }

    @Override
    public void close() {
        dispatcher.shutdown();
    }

    private Runnable dispatch(Queue<Supplier<R>> tasks) {
        return () -> {
            Supplier<R> task;
            while ((task = tasks.poll()) != null && !Thread.currentThread().isInterrupted()) {

                try {
                    if (isFailed) {
                        pending.forEach(f -> f.cancel(true));
                        break;
                    }
                    runNext(task);
                } catch (Exception e) {
                    closeAndCompleteRemaining(e);
                    break;
                }
            }
        };
    }

    private void runNext(Supplier<R> task) {
        supplyAsync(task, executor)
          .whenComplete((r, throwable) -> {
              CompletableFuture<R> next = Objects.requireNonNull(pending.poll());
              if (throwable == null) {
                  next.complete(r);
              } else {
                  next.completeExceptionally(throwable);
                  isFailed = true;
              }
          });
    }

    void closeAndCompleteRemaining(Exception e) {
        pending.forEach(future -> future.completeExceptionally(e));
    }

    Function<List<CompletableFuture<R>>, CompletableFuture<C>> foldLeftFutures() {
        return futures -> futures.stream()
          .reduce(completedFuture(collectionFactory.get()),
            accumulatingResults(),
            mergingPartialResults());
    }
}
