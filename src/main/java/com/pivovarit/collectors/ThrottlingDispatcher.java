package com.pivovarit.collectors;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

final class ThrottlingDispatcher<T> extends Dispatcher<T> {

    private final Semaphore limiter;

    ThrottlingDispatcher(Executor executor, int permits) {
        super(executor);
        this.limiter = new Semaphore(permits);
    }

    @Override
    protected Runnable dispatchStrategy() {
        return () -> {
            Supplier<T> task;
            while ((task = workingQueue.poll()) != null && !Thread.currentThread().isInterrupted()) {
                try {
                    limiter.acquire();
                    if (isMarkedFailed()) {
                        cancelPending();
                        break;
                    }
                    run(task, limiter::release);
                } catch (InterruptedException e) {
                    completePending(e);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    completePending(e);
                    break;
                }
            }
        };
    }
}
