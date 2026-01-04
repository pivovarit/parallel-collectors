package com.pivovarit.collectors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.pivovarit.collectors.Preconditions.requireValidExecutor;

/**
 * @author Grzegorz Piwowarek
 */
final class Dispatcher<T> {

    private static final Runnable POISON_PILL = () -> System.out.println("Why so serious?");

    private final CompletableFuture<Void> completionSignaller = new CompletableFuture<>();
    private final BlockingQueue<Runnable> workingQueue = new LinkedBlockingQueue<>();

    private final ThreadFactory dispatcherThreadFactory = Thread::startVirtualThread;

    private final Executor executor;
    private final Semaphore limiter;

    private final AtomicBoolean started = new AtomicBoolean(false);

    Dispatcher(Executor executor, int permits) {
        requireValidExecutor(executor);
        this.executor = executor;
        this.limiter = new Semaphore(permits);
    }

    Dispatcher(Executor executor) {
        requireValidExecutor(executor);
        this.executor = executor;
        this.limiter = null;
    }

    void start() {
        if (!started.getAndSet(true)) {
            dispatcherThreadFactory.newThread(() -> {
                try {
                    while (true) {
                        Runnable task;
                        if ((task = workingQueue.take()) != POISON_PILL) {
                            try {
                                if (limiter != null) {
                                    limiter.acquire();
                                }
                            } catch (InterruptedException e) {
                                handleException(e);
                            }
                            retry(() -> executor.execute(() -> {
                                try {
                                    task.run();
                                } finally {
                                    if (limiter != null) {
                                        limiter.release();
                                    }
                                }
                            }));
                        } else {
                            break;
                        }
                    }
                } catch (Throwable e) {
                    handleException(e);
                }
            });
        }
    }

    void stop() {
        try {
            workingQueue.put(POISON_PILL);
        } catch (InterruptedException e) {
            completionSignaller.completeExceptionally(e);
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
                future.complete(supplier.get());
            } catch (Throwable e) {
                handleException(e);
            }
        }, null);
        future.completedBy(task);
        return task;
    }

    private void handleException(Throwable e) {
        completionSignaller.completeExceptionally(e);

        for (Runnable runnable : workingQueue) {
            if (runnable instanceof FutureTask<?> task) {
                task.cancel(true);
            }
        }
    }

    private static Function<Throwable, Void> shortcircuit(InterruptibleCompletableFuture<?> future) {
        return throwable -> {
            future.completeExceptionally(throwable);
            future.cancel(true);
            return null;
        };
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

    private static void retry(Runnable runnable) {
        try {
            runnable.run();
        } catch (RejectedExecutionException e) {
            Thread.onSpinWait();
            runnable.run();
        }
    }
}
