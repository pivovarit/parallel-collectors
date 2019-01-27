package com.pivovarit.collectors;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
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

    private final ExecutorService dispatcher = newSingleThreadExecutor(new CustomThreadFactory());

    private final BlockingQueue<Supplier<R>> taskQueue = new LinkedBlockingQueue<>();
    private final ConcurrentLinkedQueue<CompletableFuture<R>> pending = new ConcurrentLinkedQueue<>();
    private final Semaphore permits;

    ThrottlingParallelCollector(
      Function<T, R> operation,
      Supplier<C> collectionFactory,
      Executor executor,
      int parallelism) {
        super(operation, collectionFactory, executor);
        permits = new Semaphore(parallelism);
        dispatcher.execute(dispatcherThread());
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> {
            CompletableFuture<R> future = new CompletableFuture<>();
            pending.offer(future);
            acc.add(future);
            taskQueue.add(() -> {
                try {
                    return operation.apply(e);
                } finally {
                    permits.release();
                }
            });
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        return super.finisher()
          .andThen(f -> {
              try {
                  return f;
              } finally {
                  dispatcher.shutdown();
              }
          });
    }

    @Override
    public void close() {
        dispatcher.shutdown();
    }

    private Runnable dispatcherThread() {
        return () -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    permits.acquire();
                    runAsyncAndComplete(taskQueue.take());
                } catch (InterruptedException e) {
                    permits.release();
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    permits.release();
                    throw e;
                }
            }
        };
    }

    private void runAsyncAndComplete(Supplier<R> task) {
        supplyAsync(task, executor)
          .thenAccept(result -> Objects.requireNonNull(pending.poll()).complete(result));
    }

    private class CustomThreadFactory implements ThreadFactory {
        private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = defaultThreadFactory.newThread(task);
            thread.setName("parallel-executor-" + thread.getName());
            thread.setDaemon(true);
            return thread;
        }
    }
}
