package com.pivovarit.collectors.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.test.Factory.boundedCollectors;

class ExecutorPollutionTest {

  @TestFactory
  Stream<DynamicTest> shouldNotPolluteExecutorFactory() {
    return boundedCollectors()
        .map(
            e ->
                DynamicTest.dynamicTest(
                    e.name(),
                    () -> {
                      try (var e1 =
                          warmedUp(
                              new ThreadPoolExecutor(
                                  1, 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(2)))) {

                        var result =
                            Stream.generate(() -> 42)
                                .limit(1000)
                                .collect(e.factory().apply(i -> i, e1, 1));

                        switch (result) {
                          case CompletableFuture<?> cf -> cf.join();
                          case Stream<?> s -> s.forEach((__) -> {});
                          default -> throw new IllegalStateException("can't happen");
                        }
                      }
                    }));
  }

  @TestFactory
  Stream<DynamicTest> shouldNotPolluteExecutorFactoryLimitedParallelism() {
    return boundedCollectors()
        .map(
            e ->
                DynamicTest.dynamicTest(
                    e.name(),
                    () -> {
                      try (var e1 =
                          warmedUp(
                              new ThreadPoolExecutor(
                                  1, 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(2)))) {

                        var result =
                            Stream.generate(() -> 42)
                                .limit(1000)
                                .collect(e.factory().apply(i -> i, e1, 2));

                        switch (result) {
                          case CompletableFuture<?> cf -> cf.join();
                          case Stream<?> s -> s.forEach((__) -> {});
                          default -> throw new IllegalStateException("can't happen");
                        }
                      }
                    }));
  }

  private static ThreadPoolExecutor warmedUp(ThreadPoolExecutor e) {
    for (int i = 0; i < e.getCorePoolSize(); i++) {
      CompletableFuture.runAsync(() -> {}).join();
    }
    return e;
  }
}
