package com.pivovarit.collectors;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class TestUtils {
  private TestUtils() {}

  public static void withExecutor(Consumer<ExecutorService> consumer) {
    try (var executorService = Executors.newCachedThreadPool()) {
      consumer.accept(executorService);
    }
  }

  public static <T> T sleepAndReturn(int millis, T value) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return value;
  }

  public static <T> T returnWithDelay(T value, Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    return value;
  }

  public static synchronized Integer incrementAndThrow(AtomicInteger counter) {
    if (counter.get() >= 10) {
      throw new IllegalArgumentException();
    }

    return counter.incrementAndGet();
  }
}
