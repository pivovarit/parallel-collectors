package com.pivovarit.collectors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.Runtime.getRuntime;

/**
 * @author Grzegorz Piwowarek
 */
final class Dispatcher<T> {

    private static final Runnable POISON_PILL = () -> System.out.println("Why so serious?");

    private final CompletableFuture<Void> completionSignaller = new CompletableFuture<>();

    private final BlockingQueue<Runnable> workingQueue = new LinkedBlockingQueue<>();

    private final ExecutorService dispatcher = newLazySingleThreadExecutor();
    private final Executor executor;
    private final Semaphore limiter;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private volatile boolean shortCircuited = false;

    private Dispatcher(Executor executor, int permits) {
        this.executor = executor;
        this.limiter = new Semaphore(permits);
    }

    static <T> Dispatcher<T> of(Executor executor, int permits) {
        return new Dispatcher<>(executor, permits);
    }

    void start() {
        if (!started.getAndSet(true)) {
            dispatcher.execute(() -> {
                try {
                    while (true) {
                        Runnable task;
                        if ((task = workingQueue.take()) != POISON_PILL) {
                            limiter.acquire();
                            executor.execute(withFinally(task, limiter::release));
                        } else {
                            break;
                        }
                    }
                } catch (Throwable e) {
                    handle(e);
                }
            });
        }
    }

    void stop() {
        try {
            workingQueue.put(POISON_PILL);
        } catch (InterruptedException e) {
            completionSignaller.completeExceptionally(e);
        } finally {
            dispatcher.shutdown();
        }
    }

    boolean isRunning() {
        return started.get();
    }

    CompletableFuture<T> enqueue(Supplier<T> supplier) {
        InterruptibleCompletableFuture<T> future = new InterruptibleCompletableFuture<>();
        workingQueue.add(completionTask(supplier, future));
        completionSignaller.exceptionally(shortcircuit(future));
        return future;
    }

    private FutureTask<Void> completionTask(Supplier<T> supplier, InterruptibleCompletableFuture<T> future) {
        FutureTask<Void> task = new FutureTask<>(() -> {
            try {
                if (!shortCircuited) {
                    future.complete(supplier.get());
                }
            } catch (Throwable e) {
                handle(e);
            }
        }, null);
        future.completedBy(task);
        return task;
    }

    private void handle(Throwable e) {
        shortCircuited = true;
        completionSignaller.completeExceptionally(e);
        dispatcher.shutdownNow();
    }

    private static Function<Throwable, Void> shortcircuit(InterruptibleCompletableFuture<?> future) {
        return throwable -> {
            future.completeExceptionally(throwable);
            future.cancel(true);
            return null;
        };
    }

    private static Runnable withFinally(Runnable task, Runnable finisher) {
        return () -> {
            try {
                task.run();
            } finally {
                finisher.run();
            }
        };
    }

    static int getDefaultParallelism() {
        return Math.max(getRuntime().availableProcessors() - 1, 4);
    }

    private static ThreadPoolExecutor newLazySingleThreadExecutor() {
        return new ThreadPoolExecutor(0, 1,
          0L, TimeUnit.MILLISECONDS,
          new SynchronousQueue<>(),
          task -> {
              Thread thread = Executors.defaultThreadFactory().newThread(task);
              thread.setName("parallel-collector-" + thread.getName());
              thread.setDaemon(false);
              return thread;
          });
    }

    static final class InterruptibleCompletableFuture<T> extends CompletableFuture<T> {
        private volatile FutureTask<?> backingTask;

        private void completedBy(FutureTask<Void> task) {
            backingTask = task;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (backingTask != null) {
                backingTask.cancel(mayInterruptIfRunning);
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }
}
