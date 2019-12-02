package com.pivovarit.collectors;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.lang.Runtime.getRuntime;

/**
 * @author Grzegorz Piwowarek
 */
final class Dispatcher<T> {

    private static final Runnable POISON_PILL = () -> System.out.println("Why so serious?");

    private final CompletableFuture<Void> completionSignaller = new CompletableFuture<>();

    private final InFlight<T> inFlight = new InFlight<>();
    private final BlockingQueue<Runnable> workingQueue = new LinkedBlockingQueue<>();

    private final ExecutorService dispatcher = newLazySingleThreadExecutor();
    private final Executor executor;
    private final Semaphore limiter;

    private volatile boolean started = false;
    private volatile boolean shortCircuited = false;

    private Dispatcher(Executor executor) {
        this(executor, getDefaultParallelism());
    }

    private Dispatcher(Executor executor, int permits) {
        this.executor = executor;
        this.limiter = new Semaphore(permits);
    }

    static <T> Dispatcher<T> limiting(Executor executor, int permits) {
        return new Dispatcher<>(executor, permits);
    }

    static <T> Dispatcher<T> limiting(Executor executor) {
        return new Dispatcher<>(executor);
    }

    CompletableFuture<Void> start() {
        started = true;
        dispatcher.execute(withExceptionHandling(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                Runnable task;
                if ((task = workingQueue.take()) != POISON_PILL) {
                    limiter.acquire();
                    executor.execute(withFinally(task, limiter::release));
                } else {
                    break;
                }
            }
            completionSignaller.complete(null);
        }));

        return completionSignaller;
    }

    void stop() {
        workingQueue.add(POISON_PILL);
        dispatcher.shutdown();
    }

    boolean isRunning() {
        return started;
    }

    CompletableFuture<T> enqueue(Supplier<T> supplier) {
        CancellableCompletableFuture<T> future = new CancellableCompletableFuture<>();
        inFlight.registerPending(future);
        workingQueue.add(future.completedBy(withExceptionHandling(() -> {
            if (!shortCircuited) {
                future.complete(supplier.get());
            }
        })));
        return future;
    }

    private Runnable withExceptionHandling(CheckedRunnable action) {
        return () -> {
            try {
                action.run();
            } catch (Exception e) {
                handle(e);
            } catch (Throwable e) {
                handle(e);
                throw e;
            }
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

    private void handle(Throwable e) {
        shortCircuited = true;
        completionSignaller.completeExceptionally(e);
        inFlight.registerException(e);
        dispatcher.shutdownNow();
    }

    @FunctionalInterface
    interface CheckedRunnable {
        void run() throws Exception;
    }

    private static int getDefaultParallelism() {
        return Math.max(getRuntime().availableProcessors() - 1, 1);
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

    static final class InFlight<T> {
        private final Queue<CancellableCompletableFuture<T>> pending = new ConcurrentLinkedQueue<>();

        void registerPending(CancellableCompletableFuture<T> future) {
            pending.add(future);
        }

        void registerException(Throwable e) {
            pending.forEach(future -> {
                future.completeExceptionally(e);
                future.cancel(true);
            });
        }
    }

    static final class CancellableCompletableFuture<T> extends CompletableFuture<T> {
        private volatile FutureTask<?> backingTask;

        private FutureTask<Void> completedBy(Runnable task) {
            FutureTask<Void> futureTask = new FutureTask<>(task, null);
            backingTask = futureTask;
            return futureTask;
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
