package com.pivovarit.collectors.blackbox;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

class CountingExecutor implements Executor {

    private final AtomicInteger counter = new AtomicInteger();

    private final Executor delegate;

    public CountingExecutor(Executor delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(Runnable command) {
        counter.incrementAndGet();
        delegate.execute(command);
    }

    public Integer getInvocations() {
        return counter.get();
    }
}
