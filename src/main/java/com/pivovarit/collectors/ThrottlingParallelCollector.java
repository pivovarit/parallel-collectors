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
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * @author Grzegorz Piwowarek
 */
@SuppressWarnings("WeakerAccess")
final class ThrottlingParallelCollector<T, R, C extends Collection<R>>
  implements Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>>, AutoCloseable {

    private final ExecutorService dispatcher = newSingleThreadExecutor(new CustomThreadFactory());
    private final Semaphore limiter;

    private volatile boolean isFailed = false;

    private final Executor executor;
    private final Queue<Supplier<R>> workingQueue;
    private final Queue<CompletableFuture<R>> pending;

    private final Function<T, R> operation;
    private final Supplier<C> collectionFactory;

    ThrottlingParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor,
      int parallelism) {
        this(operation, collection, executor, new ConcurrentLinkedQueue<>(), new ConcurrentLinkedQueue<>(), parallelism);
    }

    ThrottlingParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor,
      Queue<Supplier<R>> workingQueue,
      Queue<CompletableFuture<R>> pending,
      int parallelism) {
        this.executor = executor;
        this.collectionFactory = collection;
        this.operation = operation;
        this.workingQueue = workingQueue;
        this.pending = pending;
        this.limiter = new Semaphore(parallelism);
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
                    limiter.acquire();
                    if (isFailed) {
                        pending.forEach(f -> f.cancel(true));
                        break;
                    }
                    runNext(task);
                } catch (InterruptedException e) {
                    closeAndCompleteRemaining(e);
                    Thread.currentThread().interrupt();
                    break;
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
              supplyWithResources(() -> throwable == null
                  ? next.complete(r)
                  : supplyWithResources(() -> next.completeExceptionally(throwable), () -> isFailed = true),
                limiter::release);
          });
    }

    private void closeAndCompleteRemaining(Exception e) {
        pending.forEach(future -> future.completeExceptionally(e));
    }

    private Function<List<CompletableFuture<R>>, CompletableFuture<C>> foldLeftFutures() {
        return futures -> futures.stream()
          .reduce(completedFuture(collectionFactory.get()),
            accumulatingResults(),
            mergingPartialResults());
    }

    private static <T1, R1 extends Collection<T1>> BinaryOperator<CompletableFuture<R1>> mergingPartialResults() {
        return (f1, f2) -> f1.thenCombine(f2, (left, right) -> {
            left.addAll(right);
            return left;
        });
    }

    private static <T1, R1 extends Collection<T1>> BiFunction<CompletableFuture<R1>, CompletableFuture<T1>, CompletableFuture<R1>> accumulatingResults() {
        return (list, object) -> list.thenCombine(object, (left, right) -> {
            left.add(right);
            return left;
        });
    }

    private static <T1> T1 supplyWithResources(Supplier<T1> supplier, Runnable action) {
        try {
            return supplier.get();
        } finally {
            action.run();
        }
    }
}
