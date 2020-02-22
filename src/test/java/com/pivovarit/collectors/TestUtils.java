package com.pivovarit.collectors;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

public final class TestUtils {
    private TestUtils() {
    }

    public static <T> T returnWithDelay(T value, Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return value;
    }

    public static Integer incrementAndThrow(LongAdder counter) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore purposefully
        }
        counter.increment();
        throw new IllegalArgumentException();
    }

    public static void runWithExecutor(Consumer<Executor> consumer, int size) {
        ExecutorService executor = Executors.newFixedThreadPool(size);

        try {
            consumer.accept(executor);
        } finally {
            executor.shutdown();
        }
    }
}
