/*
 * Copyright 2014-2026 Grzegorz Piwowarek, https://4comprehension.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pivovarit.collectors;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

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
        var list = Arrays.asList(1, 2, 3);

        var result = list.stream()
          .map(i -> CompletableFuture.supplyAsync(() -> i))
          .collect(ParallelCollectors.toFuture(toList()));

        assertThat(result.join()).containsExactlyElementsOf(list);
    }

    @Test
    void shouldShortcircuit() {
        var list = IntStream.range(0, 10).boxed().toList();

        try (var e = Executors.newFixedThreadPool(10)) {
            CompletableFuture<List<Integer>> result
              = list.stream()
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
}
