package com.pivovarit.collectors;

import java.util.concurrent.Executor;

/**
 * @author Grzegorz Piwowarek
 */
final class UnboundedDispatcher<T> extends Dispatcher<T> {
    UnboundedDispatcher(Executor executor) {
        super(executor);
    }

    @Override
    protected Runnable dispatchStrategy() {
        return () -> {
            Runnable task;
            try {
                while (!Thread.currentThread().isInterrupted()
                  && isRunning()
                  && (task = getWorkingQueue().poll()) != null) {
                    run(task);
                }
            } catch (Exception e) {
                completePending(e);
            }
        };
    }
}
