package com.pivovarit.collectors;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * @author Grzegorz Piwowarek
 */
final class ThrottlingParallelCollector<T, R, C extends Collection<R>>
  extends AbstractParallelCollector<T, R, C>
  implements AutoCloseable {

    private final ParallelDispatcher<R> dispatcher;

    private final Semaphore limiter;
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
      Queue<CompletableFuture<R>> pendingQueue,
      int parallelism) {
        this.dispatcher = new ParallelDispatcher<>(executor, workingQueue, pendingQueue, this::dispatch);
        this.collectionFactory = collection;
        this.operation = operation;
        this.limiter = new Semaphore(parallelism);
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> {
            CompletableFuture<R> future = new CompletableFuture<>();
            dispatcher.addPending(future);
            dispatcher.addTask(() -> dispatcher.isMarkedFailed() ? null : operation.apply(e));
            acc.add(future);
        };
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        if (dispatcher.isNotEmpty()) {
            dispatcher.start();
            return foldLeftFutures(collectionFactory).andThen(f -> supplyWithResources(() -> f, dispatcher::close));
        } else {
            return supplyWithResources(() -> (__) -> completedFuture(collectionFactory
              .get()), dispatcher::close);
        }
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }

    @Override
    public void close() {
        dispatcher.close();
    }

    private Runnable dispatch(Queue<Supplier<R>> tasks) {
        return () -> {
            Supplier<R> task;
            while ((task = tasks.poll()) != null && !Thread.currentThread().isInterrupted()) {

                try {
                    limiter.acquire();
                    if (dispatcher.isMarkedFailed()) {
                        dispatcher.cancelAll();
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
        dispatcher.supply(task)
          .whenComplete((r, throwable) -> {
              CompletableFuture<R> next = Objects.requireNonNull(dispatcher.nextPending());
              supplyWithResources(() -> throwable == null
                  ? next.complete(r)
                  : supplyWithResources(() -> next.completeExceptionally(throwable), dispatcher::markFailed),
                limiter::release);
          });
    }

    private void closeAndCompleteRemaining(Exception e) {
        dispatcher.closeExceptionally(e);
    }
}
