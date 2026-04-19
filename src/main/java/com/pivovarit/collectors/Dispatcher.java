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

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

final class Dispatcher {

    private static final Runnable END = () -> {
    };

    private final Executor executor;
    private final Semaphore limiter;
    private final Function<Runnable, Runnable> taskDecorator;
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private final Map<CompletableFuture<?>, Thread> running = new ConcurrentHashMap<>();
    private final AtomicBoolean failed = new AtomicBoolean();
    private final AtomicReference<Throwable> primaryException = new AtomicReference<>();

    Dispatcher(Executor executor, Integer parallelism, Function<Runnable, Runnable> taskDecorator) {
        this.executor = executor;
        this.limiter = parallelism != null ? new Semaphore(parallelism) : null;
        this.taskDecorator = taskDecorator;
    }

    void start() {
        Thread.startVirtualThread(this::run);
    }

    <R> CompletableFuture<R> enqueue(Callable<R> task) {
        CompletableFuture<R> cf = new CompletableFuture<>();
        if (failed.get()) {
            cf.completeExceptionally(new CancellationException());
            return cf;
        }
        queue.offer(() -> dispatch(task, cf));
        return cf;
    }

    void close() {
        queue.offer(END);
    }

    Throwable primaryException() {
        return primaryException.get();
    }

    private <R> void dispatch(Callable<R> task, CompletableFuture<R> cf) {
        if (failed.get()) {
            cf.completeExceptionally(new CancellationException());
            return;
        }

        if (limiter != null) {
            try {
                limiter.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cf.completeExceptionally(e);
                return;
            }
        }

        if (failed.get()) {
            if (limiter != null) {
                limiter.release();
            }
            cf.completeExceptionally(new CancellationException());
            return;
        }

        Runnable body = () -> {
            Thread self = Thread.currentThread();
            running.put(cf, self);
            try {
                if (failed.get()) {
                    cf.completeExceptionally(new CancellationException());
                    return;
                }
                cf.complete(task.call());
            } catch (Throwable t) {
                cf.completeExceptionally(t);
                onFailure(t);
            } finally {
                running.remove(cf);
                if (limiter != null) {
                    limiter.release();
                }
            }
        };

        if (taskDecorator != null) {
            body = taskDecorator.apply(body);
        }

        try {
            executor.execute(body);
        } catch (Throwable t) {
            if (limiter != null) {
                limiter.release();
            }
            cf.completeExceptionally(t);
            onFailure(t);
        }
    }

    private void onFailure(Throwable cause) {
        if (failed.compareAndSet(false, true)) {
            primaryException.set(cause);
            for (Thread t : running.values()) {
                t.interrupt();
            }
        }
    }

    private void run() {
        try {
            while (true) {
                Runnable action = queue.take();
                if (action == END) {
                    return;
                }
                action.run();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
