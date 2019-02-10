package com.pivovarit.collectors;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.pivovarit.collectors.CollectorUtils.supplyWithResources;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * @author Grzegorz Piwowarek
 */
final class ThrottlingParallelCollector<T, R, C extends Collection<R>>
  extends UnboundedParallelCollector<T, R, C>
  implements AutoCloseable {

    private final Semaphore limiter;

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
        super(operation, collection, executor, workingQueue, pending);
        this.limiter = new Semaphore(parallelism);
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
}
