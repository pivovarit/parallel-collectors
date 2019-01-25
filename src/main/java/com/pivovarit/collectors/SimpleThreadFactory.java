package com.pivovarit.collectors;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

class SimpleThreadFactory implements ThreadFactory {
    private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
    private final String suffix;

    SimpleThreadFactory(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public Thread newThread(Runnable task) {
        Thread thread = defaultThreadFactory.newThread(task);
        thread.setName(thread.getName() + "-" + suffix);
        return thread;
    }
}
