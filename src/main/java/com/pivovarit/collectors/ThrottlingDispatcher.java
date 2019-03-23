package com.pivovarit.collectors;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

/**
 * @author Grzegorz Piwowarek
 */
final class ThrottlingDispatcher<T> extends Dispatcher<T> {

    private final Semaphore limiter;

    ThrottlingDispatcher(Executor executor, int permits) {
        super(executor);
        this.limiter = new Semaphore(permits);
    }

    @Override
    protected Runnable dispatchStrategy() {
        return () -> {
            Runnable task;
            try {
                while (!Thread.currentThread().isInterrupted()
                  && isRunning()
                  && (task = getWorkingQueue().poll()) != null) {
                    limiter.acquire();
                    run(task, limiter::release);
                }
            } catch (Exception e) { // covers InterruptedException
                completePending(e);
            }
        };
    }
}
