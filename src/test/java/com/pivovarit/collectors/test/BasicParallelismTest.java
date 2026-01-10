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
package com.pivovarit.collectors.test;

import com.pivovarit.collectors.TestUtils;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.test.Factory.allBounded;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class BasicParallelismTest {

    @TestFactory
    Stream<DynamicTest> shouldProcessEmptyWithMaxParallelism() {
        return Stream.of(1, 2, 4, 8, 16, 32, 64, 100)
          .flatMap(p -> allBounded()
            .map(c -> DynamicTest.dynamicTest("%s (parallelism: %d)".formatted(c.name(), p), () -> {
                assertThat(Stream.<Integer>empty().collect(c.factory().collector(i -> i, p))).isEmpty();
            })));
    }

    @TestFactory
    Stream<DynamicTest> shouldProcessAllElementsWithMaxParallelism() {
        return Stream.of(1, 2, 4, 8, 16, 32, 64, 100)
          .flatMap(p -> allBounded()
            .map(c -> DynamicTest.dynamicTest("%s (parallelism: %d)".formatted(c.name(), p), () -> {
                var list = IntStream.range(0, 100).boxed().toList();
                List<Integer> result = list.stream().collect(c.factory().collector(i -> i, p));
                assertThat(result).containsExactlyInAnyOrderElementsOf(list);
            })));
    }

    @TestFactory
    Stream<DynamicTest> shouldRespectMaxParallelism() {
        return allBounded()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              var counter = new AtomicInteger(0);
              var parallelism = 4;

              assertThatCode(() -> {
                  IntStream.range(0, 100).boxed()
                    .collect(c.factory().collector(i -> {
                        int value = counter.incrementAndGet();
                        if (value > parallelism) {
                            throw new IllegalStateException("more than two tasks executing at once!");
                        }
                        Integer result = TestUtils.returnWithDelay(i, Duration.ofMillis(10));
                        counter.decrementAndGet();
                        return result;
                    }, parallelism))
                    .forEach(i -> {});
              }).doesNotThrowAnyException();
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldRejectInvalidParallelism() {
        return allBounded()
          .flatMap(c -> Stream.of(-1, 0)
            .map(p -> DynamicTest.dynamicTest("%s [p=%d]".formatted(c.name(), p), () -> {
                assertThatThrownBy(() -> Stream.of(1).collect(c.factory().collector(i -> i, p)))
                  .isExactlyInstanceOf(IllegalArgumentException.class);
            })));
    }
}
