package com.pivovarit.collectors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
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
    private final ExecutorService dispatcher = Executors.newSingleThreadExecutor();
    private final Executor executor;
    private final Semaphore limiter;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private volatile boolean shortCircuited = false;

    private Dispatcher(Executor executor, int permits) {
        this.executor = executor;
        this.limiter = new Semaphore(permits);
    }

    static <T> Dispatcher<T> from(Executor executor, int permits) {
        return new Dispatcher<>(executor, permits);
    }

    void start() {
        if (!started.getAndSet(true)) {
            dispatcher.execute(() -> {
                try {
                    while (true) {
                        try {
                            if (limiter != null) {
                                limiter.acquire();
                            }
                        } catch (InterruptedException e) {
                            handle(e);
                        }
                        Runnable task;
                        if ((task = workingQueue.take()) != POISON_PILL) {
                            executor.execute(() -> {
                                try {
                                    task.run();
                                } finally {
                                    if (limiter != null) {
                                        limiter.release();
                                    }
                                }
                            });
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

    private void withLimiter(Supplier<T> supplier, InterruptibleCompletableFuture<T> future) throws InterruptedException {
        try {
            limiter.acquire();
            future.complete(supplier.get());
        } finally {
            limiter.release();
        }
    }

    private static Function<Throwable, Void> shortcircuit(InterruptibleCompletableFuture<?> future) {
        return throwable -> {
            future.completeExceptionally(throwable);
            future.cancel(true);
            return null;
        };
    }

    static int getDefaultParallelism() {
        return Math.max(getRuntime().availableProcessors() - 1, 4);
    }

    static final class InterruptibleCompletableFuture<T> extends CompletableFuture<T> {

        private volatile FutureTask<?> backingTask;

        private void completedBy(FutureTask<?> task) {
            backingTask = task;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            FutureTask<?> task = backingTask;
            if (task != null) {
                task.cancel(mayInterruptIfRunning);
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }
}
