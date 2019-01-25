package com.pivovarit.collectors;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

class ThreadFactoryNameDecorator implements ThreadFactory {
    private final ThreadFactory defaultThreadFactory;
    private final String prefix;

    ThreadFactoryNameDecorator(String prefix) {
        this(Executors.defaultThreadFactory(), prefix);
    }

    ThreadFactoryNameDecorator(ThreadFactory threadFactory, String prefix) {
        this.defaultThreadFactory = threadFactory;
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable task) {
        Thread thread = defaultThreadFactory.newThread(task);
        thread.setName(prefix + "-" + thread.getName());
        return thread;
    }
}
