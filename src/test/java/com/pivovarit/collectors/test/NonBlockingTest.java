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

import com.pivovarit.collectors.ParallelCollectors;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.TestUtils.returnWithDelay;
import static com.pivovarit.collectors.test.Factory.GenericCollector.asyncCollector;
import static com.pivovarit.collectors.test.Factory.e;
import static java.time.Duration.ofDays;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class NonBlockingTest {

    private static Stream<Factory.GenericCollector<Factory.AsyncCollectorFactory<Integer, Integer>>> allAsync() {
        return Stream.of(
          asyncCollector("parallel()", f -> collectingAndThen(ParallelCollectors.parallel(f), c -> c.thenApply(Stream::toList))),
          asyncCollector("parallel(toList())", f -> ParallelCollectors.parallel(f, toList())),
          asyncCollector("parallel(toList(), e)", f -> ParallelCollectors.parallel(f, c -> c.executor(e()), toList())),
          asyncCollector("parallel(toList(), e, p=1)", f -> ParallelCollectors.parallel(f, c -> c.executor(e()).parallelism(1), toList())),
          asyncCollector("parallel(toList(), e, p=2)", f -> ParallelCollectors.parallel(f, c -> c.executor(e()).parallelism(2), toList())),
          asyncCollector("parallel(toList(), e, p=1) [batching]", f -> ParallelCollectors.parallel(f, c -> c.batching().executor(e()).parallelism(1), toList())),
          asyncCollector("parallel(toList(), e, p=2) [batching]", f -> ParallelCollectors.parallel(f, c -> c.batching().executor(e()).parallelism(2), toList()))
        );
    }

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
