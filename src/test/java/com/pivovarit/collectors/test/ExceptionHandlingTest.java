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
        .map(
            c ->
                DynamicTest.dynamicTest(
                    c.name(),
                    () -> {
                      assertThatThrownBy(
                              () ->
                                  IntStream.range(0, 10)
                                      .boxed()
                                      .collect(
                                          c.factory()
                                              .collector(
                                                  i -> {
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
        .map(
            c ->
                DynamicTest.dynamicTest(
                    c.name(),
                    () -> {
                      List<Integer> elements = IntStream.range(0, 100).boxed().toList();
                      AtomicInteger counter = new AtomicInteger();

                      assertThatThrownBy(
                              () ->
                                  elements.stream()
                                      .collect(
                                          c.factory().collector(i -> incrementAndThrow(counter))))
                          .isInstanceOf(CompletionException.class)
                          .hasCauseExactlyInstanceOf(IllegalArgumentException.class);

                      assertThat(counter.longValue()).isLessThan(elements.size());
                    }));
  }
}
