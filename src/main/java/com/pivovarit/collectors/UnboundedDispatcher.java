package com.pivovarit.collectors;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

final class UnboundedDispatcher<T> extends Dispatcher<T> {
    UnboundedDispatcher(Executor executor) {
        super(executor);
    }

    @Override
    protected Runnable dispatchStrategy() {
        return () -> {
            Supplier<T> task;
            while ((task = workingQueue.poll()) != null && !Thread.currentThread().isInterrupted()) {
                try {
                    if (isMarkedFailed()) {
                        cancelPending();
                        break;
                    }
                    run(task);
                } catch (Exception e) {
                    completePending(e);
                    break;
                }
            }
        };
    }
}
