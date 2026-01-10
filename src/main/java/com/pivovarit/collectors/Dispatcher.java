/*
 * Copyright 2014-2026 Grzegorz Piwowarek, https://4comprehension.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pivovarit.collectors;

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

    private final CompletableFuture<Void> completionSignaller = new CompletableFuture<>();
    private final BlockingQueue<DispatchItem> workingQueue = new LinkedBlockingQueue<>();

    private final ThreadFactory dispatcherThreadFactory = Thread.ofVirtual()
      .name("parallel-collectors-dispatcher-",0)
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
                                    completionSignaller.completeExceptionally(e);
                                    return;
                                }
                                try {
                                    retry(() -> executor.execute(() -> {
                                        try {
                                            task.run();
                                        } finally {
                                            if (limiter != null) {
                                                limiter.release();
                                            }
                                        }
                                    }));
                                } catch (RejectedExecutionException e) {
                                    if (limiter != null) {
                                        limiter.release();
                                    }

                                    throw e;
                                }
                            }
                            case DispatchItem.Stop ignored -> {
                                return;
                            }
                        }
                    }
                } catch (Throwable e) {
                    completionSignaller.completeExceptionally(e);
                }
            }).start();
        }
    }

    void stop() {
        try {
            workingQueue.put(DispatchItem.Stop.POISON_PILL);
        } catch (InterruptedException e) {
            completionSignaller.completeExceptionally(e);
        }
    }

    boolean isRunning() {
        return started.get();
    }

    CompletableFuture<T> submit(Supplier<T> supplier) {
        InterruptibleCompletableFuture<T> future = new InterruptibleCompletableFuture<>();
        completionSignaller.whenComplete((result, ex) -> {
            if (ex != null) {
                future.completeExceptionally(ex);
                future.cancel(true);
            }
        });
        var task = new FutureTask<>(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable e) {
                completionSignaller.completeExceptionally(e);
            }
        }, null);
        future.completedBy(task);
        workingQueue.add(new DispatchItem.Task(task));
        return future;
    }

    static final class InterruptibleCompletableFuture<T> extends CompletableFuture<T> {

        private volatile FutureTask<?> backingTask;

        private void completedBy(FutureTask<?> task) {
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
