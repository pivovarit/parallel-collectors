package com.pivovarit.collectors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * @author Grzegorz Piwowarek
 */
final class Dispatcher<T> {

    private final CompletableFuture<Void> completionSignaller = new CompletableFuture<>();
    private final Executor executor;
    private final Semaphore limiter;

    private Dispatcher() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.limiter = null;
    }

    private Dispatcher(Executor executor, int permits) {
        this.executor = executor;
        this.limiter = new Semaphore(permits);
    }

    static <T> Dispatcher<T> from(Executor executor, int permits) {
        return new Dispatcher<>(executor, permits);
    }

    static <T> Dispatcher<T> virtual() {
        return new Dispatcher<>();
    }

    CompletableFuture<T> enqueue(Supplier<T> supplier) {
        InterruptibleCompletableFuture<T> future = new InterruptibleCompletableFuture<>();
        completionSignaller.whenComplete(shortcircuit(future));
        try {
            executor.execute(completionTask(supplier, future));
        } catch (Throwable e) {
            completionSignaller.completeExceptionally(e);
            return CompletableFuture.failedFuture(e);
        }
        return future;
    }

    private FutureTask<T> completionTask(Supplier<T> supplier, InterruptibleCompletableFuture<T> future) {
        FutureTask<T> task = new FutureTask<>(() -> {
            if (!completionSignaller.isCompletedExceptionally()) {
                try {
                    if (limiter == null) {
                        future.complete(supplier.get());
                    } else {
                        try {
                            limiter.acquire();
                            future.complete(supplier.get());
                        } finally {
                            limiter.release();
                        }
                    }
                } catch (Throwable e) {
                    completionSignaller.completeExceptionally(e);
                }
            }
        }, null);
        future.completedBy(task);
        return task;
    }

    private static <T> BiConsumer<T, Throwable> shortcircuit(InterruptibleCompletableFuture<?> future) {
        return (__, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
                future.cancel(true);
            }
        };
    }

    static final class InterruptibleCompletableFuture<T> extends CompletableFuture<T> {

        private volatile FutureTask<T> backingTask;

        private void completedBy(FutureTask<T> task) {
            backingTask = task;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            var task = backingTask;
            if (task != null) {
                task.cancel(mayInterruptIfRunning);
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }
}
