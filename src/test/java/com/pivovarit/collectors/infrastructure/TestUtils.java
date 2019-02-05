package com.pivovarit.collectors.infrastructure;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
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

    public static long expectedDuration(long parallelism, long unitsOfWork, long singleJobDuration) {
        if (unitsOfWork < parallelism) {
            return singleJobDuration;
        } else if (unitsOfWork % parallelism == 0) {
            return (unitsOfWork / parallelism) * singleJobDuration;
        } else {
            return (unitsOfWork / parallelism + 1) * singleJobDuration;
        }
    }

    public static Object incrementAndThrow(LongAdder counter) {
        counter.increment();
        throw new IllegalArgumentException();
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


    public static void runWithExecutor(Consumer<Executor> consumer, int size) {
        ExecutorService executor = Executors.newFixedThreadPool(size);

        try {
            consumer.accept(executor);
        } finally {
            executor.shutdownNow();
        }
    }
}
