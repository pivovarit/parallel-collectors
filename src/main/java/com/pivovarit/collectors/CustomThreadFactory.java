package com.pivovarit.collectors;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author Grzegorz Piwowarek
 */
class CustomThreadFactory implements ThreadFactory {
    private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

    @Override
    public Thread newThread(Runnable task) {
        Thread thread = defaultThreadFactory.newThread(task);
        thread.setName("parallel-collector-" + thread.getName());
        thread.setDaemon(true);
        return thread;
    }
}
