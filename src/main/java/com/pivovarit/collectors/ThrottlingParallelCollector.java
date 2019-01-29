package com.pivovarit.collectors;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * @author Grzegorz Piwowarek
 */
class ThrottlingParallelCollector<T, R, C extends Collection<R>>
  extends AbstractParallelCollector<T, R, C>
  implements AutoCloseable {

    private final Semaphore limiter;

    ThrottlingParallelCollector(
      Function<T, R> operation,
      Supplier<C> collectionFactory,
      Executor executor,
      int parallelism) {
        super(operation, collectionFactory, executor);
        this.limiter = new Semaphore(parallelism);
    }

    ThrottlingParallelCollector(Function<T, R> operation, Supplier<C> collection, Executor executor, int parallelism, Queue<Supplier<R>> workingQueue, Queue<CompletableFuture<R>> pending) {
        super(operation, collection, executor, workingQueue, pending);
        this.limiter =  new Semaphore(parallelism);
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> {
            CompletableFuture<R> future = new CompletableFuture<>();
            pending.add(future);
            workingQueue.add(() -> {
                try {
                    return operation.apply(e);
                } finally {
                    limiter.release();
                }
            });

            acc.add(future);
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }

    @Override
    public void close() {
        dispatcher.shutdown();
    }

    @Override
    protected Runnable dispatch(Queue<Supplier<R>> tasks) {
        return () -> {
            Supplier<R> task;
            while ((task = tasks.poll()) != null && !Thread.currentThread().isInterrupted()) {
                try {
                    limiter.acquire();
                    runAsyncAndComplete(task);
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

    private CompletableFuture<R> getNextFuture() {
        CompletableFuture<R> future;
        do {
            future = pending.poll();
        } while (future == null);
        return future;
    }

    private void closeAndCompleteRemaining(Exception e) {
        limiter.release();
        pending.forEach(future -> future.completeExceptionally(e));
    }

    private void runAsyncAndComplete(Supplier<R> task) {
        supplyAsync(task, executor)
          .handle((r, throwable) -> {
              CompletableFuture<R> nextFuture = getNextFuture();
              return throwable == null
                ? nextFuture.complete(r)
                : nextFuture.completeExceptionally(throwable);
          });
    }
}
