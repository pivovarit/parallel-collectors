package com.pivovarit.collectors;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

class ThreadFactoryNameDecorator implements ThreadFactory {
    private final ThreadFactory defaultThreadFactory;
    private final String suffix;

    ThreadFactoryNameDecorator(String suffix) {
        this.suffix = suffix;
        this.defaultThreadFactory = Executors.defaultThreadFactory();
    }

    ThreadFactoryNameDecorator(ThreadFactory threadFactory, String suffix) {
        this.defaultThreadFactory = threadFactory;
        this.suffix = suffix;
    }

    @Override
    public Thread newThread(Runnable task) {
        Thread thread = defaultThreadFactory.newThread(task);
        thread.setName(thread.getName() + "-" + suffix);
        return thread;
    }
}
