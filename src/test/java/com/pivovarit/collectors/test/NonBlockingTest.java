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
import com.pivovarit.collectors.test.Factory.GenericCollector;
import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.TestUtils.returnWithDelay;
import static java.time.Duration.ofDays;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class NonBlockingTest {

    private static Stream<GenericCollector<Factory.AsyncCollectorFactory<Integer, Integer>>> allAsync() {
        return Stream.of(
          new GenericCollector<Factory.AsyncCollectorFactory<Integer, Integer>>("parallel()", f -> Collectors.collectingAndThen(ParallelCollectors.parallel(f), c -> c.thenApply(Stream::toList))),
          new GenericCollector<Factory.AsyncCollectorFactory<Integer, Integer>>("parallel(toList())", f6 -> ParallelCollectors.parallel(f6, Collectors.toList())),
          new GenericCollector<Factory.AsyncCollectorFactory<Integer, Integer>>("parallel(toList(), e)", f5 -> ParallelCollectors.parallel(f5, c5 -> c5.executor(Factory.e()), Collectors.toList())),
          new GenericCollector<Factory.AsyncCollectorFactory<Integer, Integer>>("parallel(toList(), e, p=1)", f4 -> ParallelCollectors.parallel(f4, c4 -> c4.executor(Factory.e()).parallelism(1), Collectors.toList())),
          new GenericCollector<Factory.AsyncCollectorFactory<Integer, Integer>>("parallel(toList(), e, p=2)", f3 -> ParallelCollectors.parallel(f3, c3 -> c3.executor(Factory.e()).parallelism(2), Collectors.toList())),
          new GenericCollector<Factory.AsyncCollectorFactory<Integer, Integer>>("parallel(toList(), e, p=1) [batching]", f2 -> ParallelCollectors.parallel(f2, c2 -> c2.batching().executor(Factory.e()).parallelism(1), Collectors.toList())),
          new GenericCollector<Factory.AsyncCollectorFactory<Integer, Integer>>("parallel(toList(), e, p=2) [batching]", f1 -> ParallelCollectors.parallel(f1, c1 -> c1.batching().executor(Factory.e()).parallelism(2), Collectors.toList()))
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
