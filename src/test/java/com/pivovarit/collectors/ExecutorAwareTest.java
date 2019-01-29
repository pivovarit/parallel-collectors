package com.pivovarit.collectors;

import org.junit.After;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Grzegorz Piwowarek
 */
public abstract class ExecutorAwareTest {
    protected volatile ThreadPoolExecutor executor;

    @After
    public void after() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }


    public static ThreadPoolExecutor threadPoolExecutor(int unitsOfWork) {
        return new ThreadPoolExecutor(unitsOfWork, unitsOfWork,
          0L, TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<>());
    }
}
