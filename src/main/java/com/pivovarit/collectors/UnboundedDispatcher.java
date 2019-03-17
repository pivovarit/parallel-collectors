package com.pivovarit.collectors;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

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
            Supplier<T> task;
            try {
                while (!Thread.currentThread().isInterrupted() && (task = getWorkingQueue().poll()) != null) {
                    if (isInterrupted()) {
                        cancelPending();
                        break;
                    }
                    run(task);
                }
            } catch (Exception e) {
                completePending(e);
            }
        };
    }
}
