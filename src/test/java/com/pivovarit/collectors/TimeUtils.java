package com.pivovarit.collectors;

import java.time.Duration;
import java.time.Instant;

public final class TimeUtils {
    private TimeUtils() {
    }

    public static long time(Runnable runnable) {
        Instant start = Instant.now();
        runnable.run();
        return Duration.between(start, Instant.now()).toMillis();
    }
}
