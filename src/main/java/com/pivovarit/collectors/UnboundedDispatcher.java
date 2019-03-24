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
    protected CheckedConsumer dispatchStrategy() {
        return this::run;
    }
}
