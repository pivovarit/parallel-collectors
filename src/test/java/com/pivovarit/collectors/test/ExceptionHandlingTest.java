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

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.TestUtils.incrementAndThrow;
import static com.pivovarit.collectors.test.Factory.all;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExceptionHandlingTest {

    @TestFactory
    Stream<DynamicTest> shouldPropagateExceptionFactory() {
        return all()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              assertThatThrownBy(() -> IntStream.range(0, 10)
                .boxed()
                .collect(c.factory().collector(i -> {
                    if (i == 7) {
                        throw new IllegalArgumentException();
                    } else {
                        return i;
                    }
                })))
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
          }));
    }

    @TestFactory
    Stream<DynamicTest> shouldShortcircuitOnException() {
        return all()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              List<Integer> elements = IntStream.range(0, 100).boxed().toList();
              AtomicInteger counter = new AtomicInteger();

              assertThatThrownBy(() -> elements.stream()
                .collect(c.factory().collector(i -> incrementAndThrow(counter))))
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class);

              assertThat(counter.longValue()).isLessThan(elements.size());
          }));
    }
}
