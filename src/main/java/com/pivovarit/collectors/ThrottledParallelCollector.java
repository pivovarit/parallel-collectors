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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * @author Grzegorz Piwowarek
 */
class ThrottledParallelCollector<T, R1, R2 extends Collection<R1>> extends AbstractParallelCollector<T, R1, R2>
  implements Collector<T, List<CompletableFuture<R1>>, CompletableFuture<R2>> {

    private final Semaphore permits;
    private final BlockingQueue<Supplier<R1>> taskQueue = new LinkedBlockingQueue<>();
    private final ConcurrentLinkedQueue<CompletableFuture<R1>> pending = new ConcurrentLinkedQueue<>();
    private final ExecutorService dispatcher = Executors.newSingleThreadExecutor();

    ThrottledParallelCollector(
      Function<T, R1> operation,
      Supplier<R2> collection,
      Executor executor,
      int parallelism) {
        super(operation, collection, executor);
        this.permits = new Semaphore(parallelism);

        dispatcher.execute(() -> {
            while (true) {
                try {
                    permits.acquire();
                    Supplier<R1> task = taskQueue.take();
                    supplyAsync(task, executor)
                      .thenAccept(result -> Objects.requireNonNull(pending.poll()).complete(result));
                } catch (InterruptedException e) {
                    permits.release();
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    @Override
    public BiConsumer<List<CompletableFuture<R1>>, T> accumulator() {
        return (acc, e) -> {
            CompletableFuture<R1> future = new CompletableFuture<>();
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
    public Function<List<CompletableFuture<R1>>, CompletableFuture<R2>> finisher() {
        return super.finisher()
          .andThen(f -> {
              dispatcher.shutdown();
              return f;
          });
    }
}
