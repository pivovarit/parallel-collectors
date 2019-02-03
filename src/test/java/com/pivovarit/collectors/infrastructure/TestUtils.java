package com.pivovarit.collectors.infrastructure;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * @author Grzegorz Piwowarek
 */
public final class TestUtils {
    private TestUtils() {
    }

    public static <T> Map.Entry<T, Long> timed(Supplier<T> runnable) {
        Instant start = Instant.now();
        return new AbstractMap.SimpleEntry<>(
          runnable.get(),
          Duration.between(start, Instant.now()).toMillis());
    }

    public static <T> T returnWithDelay(T value, Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return value;
    }


    public static Object incrementAndThrow(LongAdder counter) {
        counter.increment();
        throw new IllegalArgumentException();
    }

    public static Integer throwing(Integer i) {
        if (i == 7) {
            throw new IllegalArgumentException();
        } else {
            return i;
        }
    }

    public static class CountingExecutor implements Executor {
        private final LongAdder longAdder = new LongAdder();

        @Override
        public void execute(Runnable command) {
            longAdder.increment();
        }

        public long count() {
            return longAdder.longValue();
        }
    }
}
