package com.pivovarit.collectors.infrastructure;

import org.junit.After;
import org.junit.jupiter.api.AfterEach;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Grzegorz Piwowarek
 */
public abstract class ExecutorAwareTest {
    protected volatile ThreadPoolExecutor executor;

    @After
    @AfterEach
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
