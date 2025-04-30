package com.pivovarit.collectors.test;

import com.pivovarit.collectors.Config;
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
          asyncCollector("parallel(toList(), e)", f -> ParallelCollectors.parallel(f, toList(), e())),
          asyncCollector("parallel(toList(), e, p=1)", f -> ParallelCollectors.parallel(f, toList(), e(), 1)),
          asyncCollector("parallel(toList(), e, p=2)", f -> ParallelCollectors.parallel(f, toList(), e(), 2)),
          asyncCollector("parallel(toList(), e, p=1) [batching]", f -> ParallelCollectors.Batching.parallel(f, toList(), e(), 1)),
          asyncCollector("parallel(toList(), e, p=2) [batching]", f -> ParallelCollectors.Batching.parallel(f, toList(), e(), 2)),
          //
          asyncCollector("parallel(toList(), e)", f -> ParallelCollectors.parallel(f, toList(), Config.with().executor(e()).build())),
          asyncCollector("parallel(toList(), e, p=1)", f -> ParallelCollectors.parallel(f, toList(), Config.with().parallelism(1).executor(e()).build())),
          asyncCollector("parallel(toList(), e, p=2)", f -> ParallelCollectors.parallel(f, toList(), Config.with().parallelism(2).executor(e()).build())),
          asyncCollector("parallel(toList(), e, p=1) [batching]", f -> ParallelCollectors.parallel(f, toList(), Config.with().batching(true).parallelism(1).executor(e()).build())),
          asyncCollector("parallel(toList(), e, p=2) [batching]", f -> ParallelCollectors.parallel(f, toList(), Config.with().batching(true).parallelism(2).executor(e()).build()))
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
