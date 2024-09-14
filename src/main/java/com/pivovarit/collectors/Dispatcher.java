package com.pivovarit.collectors;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Grzegorz Piwowarek
 */
final class Dispatcher<T> {

    private static final Runnable POISON_PILL = () -> System.out.println("Why so serious?");

    private final CompletableFuture<Void> completionSignaller = new CompletableFuture<>();
    private final BlockingQueue<Runnable> workingQueue = new LinkedBlockingQueue<>();

    private final Executor executor;
    private final Semaphore limiter;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private volatile boolean shortCircuited = false;

    private Dispatcher(Configuration configuration) {
        if (configuration.maxParallelism == null) {
            this.limiter = null;
        } else {
            requireValidMaxParallelism(configuration.maxParallelism);
            this.limiter = new Semaphore(configuration.maxParallelism);
        }

        this.executor = Objects.requireNonNullElseGet(requireValidExecutor(configuration.executor), Dispatcher::defaultExecutorService);
    }

    static <T> Dispatcher<T> virtual() {
        return new Dispatcher<>(Configuration.initial());
    }

    static <T> Dispatcher<T> virtual(int maxParallelism) {
        return new Dispatcher<>(Configuration.initial().withMaxParallelism(maxParallelism));
    }

    static <T> Dispatcher<T> from(Configuration configuration) {
        return new Dispatcher<>(configuration);
    }

    void start() {
        if (!started.getAndSet(true)) {
            Thread.startVirtualThread(() -> {
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
    }

    private static Function<Throwable, Void> shortcircuit(InterruptibleCompletableFuture<?> future) {
        return throwable -> {
            future.completeExceptionally(throwable);
            future.cancel(true);
            return null;
        };
    }

    record Configuration(Executor executor, Integer maxParallelism, ThreadFactory dispatcherFactory) {

        public static Configuration initial() {
            return new Configuration(null, null, null);
        }
        public Configuration withExecutor(Executor executor) {
            return new Configuration(executor, this.maxParallelism, this.dispatcherFactory);
        }

        public Configuration withMaxParallelism(int permits) {
            return new Configuration(this.executor, permits, this.dispatcherFactory);
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
    private static ExecutorService defaultExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    static void requireValidMaxParallelism(int maxParallelism) {
        if (maxParallelism < 1) {
            throw new IllegalArgumentException("Max parallelism can't be lower than 1");
        }
    }

    private static Executor requireValidExecutor(Executor executor) {
        if (executor instanceof ThreadPoolExecutor tpe) {
            switch (tpe.getRejectedExecutionHandler()) {
                case ThreadPoolExecutor.DiscardPolicy __ ->
                  throw new IllegalArgumentException("Executor's RejectedExecutionHandler can't discard tasks");
                case ThreadPoolExecutor.DiscardOldestPolicy __ ->
                  throw new IllegalArgumentException("Executor's RejectedExecutionHandler can't discard tasks");
                default -> {
                    // no-op
                }
            }
        }
        return executor;
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
