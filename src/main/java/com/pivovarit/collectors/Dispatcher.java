package com.pivovarit.collectors;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
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

    private final ExecutorService dispatcher = newLazySingleThreadExecutor();

    private final Queue<CompletableFuture<T>> pending = new ConcurrentLinkedQueue<>();
    private final Queue<Future<Void>> cancellables = new ConcurrentLinkedQueue<>();
    private final BlockingQueue<Runnable> workingQueue = new LinkedBlockingQueue<>();
    private final Executor executor;
    private final Semaphore limiter;

    private volatile boolean started = false;
    private volatile boolean shortCircuited = false;

    Dispatcher(Executor executor) {
        this(executor, getDefaultParallelism());
    }

    Dispatcher(Executor executor, int permits) {
        this.executor = executor;
        this.limiter = new Semaphore(permits);
    }

    CompletableFuture<Void> start() {
        started = true;
        dispatcher.execute(withExceptionHandling(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                Runnable task = workingQueue.take();
                if (task == POISON_PILL) {
                    break;
                }

                limiter.acquire();
                executor.execute(cancellable(combined(task, limiter::release)));
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
        CompletableFuture<T> future = new CompletableFuture<>();
        pending.add(future);
        workingQueue.add(withExceptionHandling(() -> {
            if (!shortCircuited) {
                future.complete(supplier.get());
            }
        }));
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

    private static Runnable combined(Runnable task, Runnable finisher) {
        return () -> {
            try {
                task.run();
            } finally {
                finisher.run();
            }
        };
    }

    private FutureTask<Void> cancellable(Runnable task) {
        FutureTask<Void> futureTask  = new FutureTask<>(task, null);
        cancellables.add(futureTask);
        return futureTask;
    }

    private void handle(Throwable e) {
        shortCircuited = true;
        completionSignaller.completeExceptionally(e);
        pending.forEach(future -> future.completeExceptionally(e));
        cancellables.forEach(future -> future.cancel(true));
        dispatcher.shutdownNow();
    }

    @FunctionalInterface
    interface CheckedRunnable<T extends Exception> {
        void run() throws T;
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
}
