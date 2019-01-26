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
import java.util.stream.Collector;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * @author Grzegorz Piwowarek
 */
class ThrottledParallelCollector<T, R, C extends Collection<R>>
  extends AbstractParallelCollector<T, R, C>
  implements Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>>, AutoCloseable {

    private final Semaphore permits;
    private final BlockingQueue<Supplier<R>> taskQueue = new LinkedBlockingQueue<>();
    private final ConcurrentLinkedQueue<CompletableFuture<R>> pending = new ConcurrentLinkedQueue<>();
    private final ExecutorService dispatcher = newSingleThreadExecutor(new ThreadFactoryNameDecorator("throttled-parallel-executor"));

    ThrottledParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor,
      int parallelism) {
        super(operation, collection, executor);
        permits = new Semaphore(parallelism);
        dispatcher.execute(dispatcherThread());
    }

    @Override
    public BiConsumer<List<CompletableFuture<R>>, T> accumulator() {
        return (acc, e) -> {
            CompletableFuture<R> future = new CompletableFuture<>();
            pending.add(future);
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
              dispatcher.shutdown();
              return f;
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
                    Supplier<R> task = taskQueue.take();
                    runAsyncAndComplete(task);
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

    private class ThreadFactoryNameDecorator implements ThreadFactory {
        private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
        private final String prefix;

        private ThreadFactoryNameDecorator(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = defaultThreadFactory.newThread(task);
            thread.setName(prefix + "-" + thread.getName());
            return thread;
        }
    }
}
