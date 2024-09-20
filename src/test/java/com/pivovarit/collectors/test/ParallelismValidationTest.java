package com.pivovarit.collectors.test;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static com.pivovarit.collectors.test.Factory.allBounded;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParallelismValidationTest {

    @TestFactory
    Stream<DynamicTest> shouldRejectInvalidRejectedExecutionHandlerFactory() {
        return allBounded()
          .map(c -> DynamicTest.dynamicTest(c.name(), () -> {
              assertThatThrownBy(() -> c.factory()
                .collector(i -> i, -1))
                .isInstanceOf(IllegalArgumentException.class);
          }));
    }
}
