package com.pivovarit.collectors;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Supplier;

public final class TimeUtils {
    private TimeUtils() {
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
}
