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
    protected CheckedConsumer dispatchStrategy() {
        return task -> {
            limiter.acquire();
            run(task, limiter::release);
        };
    }
}
