package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.pivovarit.collectors.Preconditions.requireValidExecutor;

/**
 * @author Grzegorz Piwowarek
 */
final class Dispatcher<T> {

    private final List<InterruptibleCompletableFuture<T>> tasks = new ArrayList<>();

    private final BlockingQueue<DispatchItem> workingQueue = new LinkedBlockingQueue<>();

    private final ThreadFactory dispatcherThreadFactory = Thread.ofVirtual()
      .name("parallel-collectors-dispatcher-", 0)
      .factory();

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
                        switch (workingQueue.take()) {
                            case DispatchItem.Task(Runnable task) -> {
                                try {
                                    if (limiter != null) {
                                        limiter.acquire();
                                    }
                                } catch (InterruptedException e) {
                                    interrupt(e);
                                    return;
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
                            }
                            case DispatchItem.Stop ignored -> {
                                return;
                            }
                        }
                    }
                } catch (Throwable e) {
                    interrupt(e);
                }
            }).start();
        }
    }

    void stop() {
        try {
            workingQueue.put(DispatchItem.Stop.POISON_PILL);
        } catch (InterruptedException e) {
            interrupt(e);
        }
    }

    boolean isRunning() {
        return started.get();
    }

    CompletableFuture<T> enqueue(Supplier<T> supplier) {
        InterruptibleCompletableFuture<T> future = new InterruptibleCompletableFuture<>();
        tasks.add(future);
        workingQueue.add(completionTask(supplier, future));
        return future;
    }

    private DispatchItem.Task completionTask(Supplier<T> supplier, InterruptibleCompletableFuture<T> future) {
        FutureTask<Void> task = new FutureTask<>(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable e) {
                interrupt(e);
            }
        }, null);
        future.completedBy(task);
        return new DispatchItem.Task(task);
    }

    private void interrupt(Throwable e) {
        for (var task : tasks) {
            task.completeExceptionally(e);
            task.cancel(true);
        }
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

    sealed interface DispatchItem permits DispatchItem.Task, DispatchItem.Stop {
        record Task(FutureTask<?> task) implements DispatchItem {
            public Task {
                Objects.requireNonNull(task);
            }
        }

        enum Stop implements DispatchItem {
            POISON_PILL
        }
    }
}
