package com.pivovarit.collectors;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * @author Grzegorz Piwowarek
 */
class ThrottlingParallelCollector<T, R, C extends Collection<R>>
  extends AbstractParallelCollector<T, R, C>
  implements AutoCloseable {

    private final Semaphore limiter;

    private final AtomicBoolean isFailed = new AtomicBoolean(false);

    ThrottlingParallelCollector(
      Function<T, R> operation,
      Supplier<C> collectionFactory,
      Executor executor,
      int parallelism) {
        super(operation, collectionFactory, executor);
        this.limiter = new Semaphore(parallelism);
    }

    ThrottlingParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor,
      int parallelism,
      Queue<Supplier<R>> workingQueue,
      Queue<CompletableFuture<R>> pending) {
        super(operation, collection, executor, workingQueue, pending);
        this.limiter = new Semaphore(parallelism);
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> {
            CompletableFuture<R> future = new CompletableFuture<>();
            pending.add(future);
            workingQueue.add(() -> isFailed.get() ? null : operation.apply(e));
            acc.add(future);
        };
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        if (workingQueue.size() != 0) {
            dispatcher.execute(dispatch(workingQueue));
            return foldLeftFutures().andThen(f -> supplyWithResources(() -> f, dispatcher::shutdown));
        } else {
            return supplyWithResources(this::foldLeftFutures, dispatcher::shutdown);
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
                    if (isFailed.get()) {
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
                  : supplyWithResources(() -> next.completeExceptionally(throwable), () -> isFailed.set(true)),
                limiter::release);
          });
    }

    private void closeAndCompleteRemaining(Exception e) {
        pending.forEach(future -> future.completeExceptionally(e));
        limiter.release();
    }

    private static <RX> RX supplyWithResources(Supplier<RX> supplier, Runnable action) {
        try {
            return supplier.get();
        } finally {
            action.run();
        }
    }
}
