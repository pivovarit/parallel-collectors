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

import java.lang.ref.Cleaner;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.pivovarit.collectors.Preconditions.requireValidExecutor;

/**
 * @author Grzegorz Piwowarek
 */
final class Dispatcher<T> {

    private static final Cleaner CLEANER = Cleaner.create();

    private final CompletableFuture<Void> completionSignaller = new CompletableFuture<>();
    private final Set<InterruptibleCompletableFuture<T>> pendingFutures = ConcurrentHashMap.newKeySet();
    private final BlockingQueue<DispatchItem> workingQueue = new LinkedBlockingQueue<>();

    private final ThreadFactory dispatcherThreadFactory = Thread.ofVirtual()
      .name("parallel-collectors-dispatcher-", 0)
      .factory();

    private final Consumer<Thread> dispatcherThreadHook;

    private final Executor executor;
    private final Semaphore limiter;

    // UNSTARTED -> RUNNING -> SHUTTING_DOWN
    enum State {
        UNSTARTED, RUNNING, SHUTTING_DOWN
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.UNSTARTED);

    Dispatcher(Executor executor, int permits, Consumer<Thread> dispatcherThreadHook) {
        requireValidExecutor(executor);
        this.executor = executor;
        this.dispatcherThreadHook = dispatcherThreadHook;
        this.limiter = new Semaphore(permits);
    }

    Dispatcher(Executor executor, int permits) {
        this(executor, permits, c -> {});
    }

    Dispatcher(Executor executor) {
        this(executor, c -> {});
    }

    Dispatcher(Executor executor, Consumer<Thread> dispatcherThreadHook) {
        requireValidExecutor(executor);
        this.executor = executor;
        this.dispatcherThreadHook = dispatcherThreadHook;
        this.limiter = null;
    }

    void start() {
        if (state.compareAndSet(State.UNSTARTED, State.RUNNING)) {
            var thread = dispatcherThreadFactory.newThread(() -> {
                try {
                    while (true) {
                        switch (workingQueue.take()) {
                            case DispatchItem.Task(Runnable task) -> {
                                try {
                                    if (limiter != null) {
                                        limiter.acquire();
                                    }
                                } catch (InterruptedException e) {
                                    abort(e);
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
                    abort(e);
                }
            });
            dispatcherThreadHook.accept(thread);
            thread.start();
        }
    }

    void stop() {
        if (state.compareAndSet(State.RUNNING, State.SHUTTING_DOWN)) {
            workingQueue.add(DispatchItem.Stop.POISON_PILL);
        }
    }

    Cleaner.Cleanable registerTerminationGuard(Object guard) {
        var queue = workingQueue;
        var currentState = state;

        return CLEANER.register(guard, () -> {
            if (currentState.compareAndSet(State.RUNNING, State.SHUTTING_DOWN)) {
                queue.add(DispatchItem.Stop.POISON_PILL);
            }
        });
    }

    boolean wasStarted() {
        return switch (state.get()) {
            case UNSTARTED -> false;
            case RUNNING, SHUTTING_DOWN -> true;
        };
    }

    boolean wasShutdown() {
        return switch (state.get()) {
            case UNSTARTED, RUNNING -> false;
            case SHUTTING_DOWN -> true;
        };
    }

    CompletableFuture<T> submit(Supplier<T> supplier) {
        if (state.get() == State.SHUTTING_DOWN) {
            throw new IllegalStateException("collector was already used and cannot be reused");
        }
        var future = new InterruptibleCompletableFuture<T>();
        var task = new FutureTask<>(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable e) {
                abort(e);
                Thread.interrupted();
            }
        }, null);
        future.completedBy(task);
        pendingFutures.add(future);
        future.whenComplete((result, ex) -> pendingFutures.remove(future));

        if (completionSignaller.isCompletedExceptionally()) {
            completionSignaller.whenComplete((result, ex) -> abort(future, ex));
        }
        workingQueue.add(new DispatchItem.Task(task));
        return future;
    }

    private void abort(Throwable e) {
        if (completionSignaller.completeExceptionally(e)) {
            for (var future : pendingFutures) {
                abort(future, e);
            }
        }
    }

    private static void abort(InterruptibleCompletableFuture<?> future, Throwable e) {
        future.completeExceptionally(e);
        future.cancel(true);
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
