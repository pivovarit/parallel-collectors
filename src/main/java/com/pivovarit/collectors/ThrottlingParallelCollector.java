package com.pivovarit.collectors;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
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
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * @author Grzegorz Piwowarek
 */
class ThrottlingParallelCollector<T, R, C extends Collection<R>>
  extends AbstractParallelCollector<T, R, C>
  implements AutoCloseable {

    private final Queue<CompletableFuture<R>> all;
    private final Semaphore limiter;
    private final AtomicBoolean failed = new AtomicBoolean(false);
    private volatile Exception exception;

    ThrottlingParallelCollector(
      Function<T, R> operation,
      Supplier<C> collectionFactory,
      Executor executor,
      int parallelism) {
        super(operation, collectionFactory, executor);
        this.limiter = new Semaphore(parallelism);
        this.all = new LinkedList<>();
    }

    ThrottlingParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor,
      int parallelism,
      Queue<Supplier<R>> workingQueue,
      Queue<CompletableFuture<R>> pending) {
        super(operation, collection, executor, workingQueue, pending);
        this.limiter =  new Semaphore(parallelism);
        this.all = new LinkedList<>();
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> {
            CompletableFuture<R> future = new CompletableFuture<>();
            pending.add(future);
            all.add(future);
            workingQueue.add(() -> {
                try {
                    return failed.get() ? null : operation.apply(e);
                } catch (Exception ex) {
                    exception = ex;
                    failed.set(true);
                    throw ex;
                }
                finally {
                    limiter.release();
                }
            });
            acc.add(future);
        };
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        if (workingQueue.size() != 0) {
            dispatcher.execute(dispatch(workingQueue));
            return foldLeftFutures().andThen(f -> {
                try {
                    return f;
                } finally {
                    dispatcher.shutdown();
                }
            });
        } else {
            try {
                return foldLeftFutures();
            } finally {
                dispatcher.shutdown();
            }
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

    @Override
    protected Runnable dispatch(Queue<Supplier<R>> tasks) {
        return () -> {
            Supplier<R> task;
            while ((task = tasks.poll()) != null && !Thread.currentThread().isInterrupted()) {

                try {
                    limiter.acquire();
                    if (failed.get()) {
                        closeAndCompleteRemaining(exception);
                        break;
                    }
                    CompletableFuture<Boolean> future = supplyAsync(task, executor)
                      .handle((r, throwable) -> {
                          CompletableFuture<R> nextFuture = getNextFuture();
                          return throwable == null
                            ? nextFuture.complete(r)
                            : nextFuture.completeExceptionally(throwable);
                      });
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
        all.forEach(future -> future.completeExceptionally(e));
    }
}
