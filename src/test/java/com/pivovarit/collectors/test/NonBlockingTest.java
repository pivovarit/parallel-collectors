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

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.test.Factory.allAsync;
import static java.time.Duration.ofDays;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class NonBlockingTest {

    @TestFactory
    Stream<DynamicTest> shouldNotBlockTheCallingThread() {
        return allAsync()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              assertTimeoutPreemptively(Duration.ofMillis(100), () -> {
                  var __ = Stream.of(1, 2, 3, 4).collect(c.factory().collector(i -> returnWithDelay(i, ofDays(1))));
              });
          }));
    }
}
