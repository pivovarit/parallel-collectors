package com.pivovarit.collectors;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class FutureCollectorsTest {

    @Test
    void shouldCollect() {
        List<Integer> list = Arrays.asList(1, 2, 3);

        CompletableFuture<List<Integer>> result = list.stream()
          .map(i -> CompletableFuture.supplyAsync(() -> i))
          .collect(ParallelCollectors.toFuture());

        assertThat(result.join()).containsExactlyElementsOf(list);
    }

    @Test
    void shouldCollectToList() {
        List<Integer> list = Arrays.asList(1, 2, 3);

        CompletableFuture<List<Integer>> result = list.stream()
          .map(i -> CompletableFuture.supplyAsync(() -> i))
          .collect(ParallelCollectors.toFuture(toList()));

        assertThat(result.join()).containsExactlyElementsOf(list);
    }

    @Test
    void shouldShortcircuit() {
        List<Integer> list = IntStream.range(0, 10).boxed().collect(toList());

        ExecutorService e = Executors.newFixedThreadPool(10);

        CompletableFuture<List<Integer>> result = list.stream()
          .map(i -> CompletableFuture.supplyAsync(() -> {
              if (i != 9) {
                  try {
                      Thread.sleep(1000);
                  } catch (InterruptedException ex) {
                      ex.printStackTrace();
                  }
                  return i;
              } else {
                  throw new RuntimeException();
              }
          }, e))
          .collect(ParallelCollectors.toFuture(toList()));

        assertTimeout(Duration.ofMillis(100), () -> {
            try {
                result.join();
            } catch (CompletionException ex) {
            }
        });
    }
}
