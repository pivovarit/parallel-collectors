package com.pivovarit.collectors;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.inParallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToList;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToSet;
import static java.util.function.Function.identity;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author Grzegorz Piwowarek
 */
class SmokeTests {

    private static final Executor executor = Executors.newFixedThreadPool(100);

    @TestFactory
    Stream<DynamicTest> testCollectors() {
        return of(
          forListCollector(inParallelToList(i -> i, executor)),
          forListCollector(inParallelToList(i -> i, executor, 2)),
          forSetCollector(inParallelToSet(i -> i, executor)),
          forSetCollector(inParallelToSet(i -> i, executor, 2)),
          forCollectionCollector(inParallelToCollection(i -> i, ArrayList::new, executor)),
          forCollectionCollector(inParallelToCollection(i -> i, ArrayList::new, executor, 2))
        ).flatMap(identity());
    }

    private Stream<DynamicTest> forCollectionCollector(Collector<Integer, List<CompletableFuture<Integer>>, CompletableFuture<Collection<Integer>>> collector) {
        return of(
          dynamicTest("inParallelToCollection should collect", () -> {
              List<Integer> elements = IntStream.range(0, 10).boxed().collect(Collectors.toList());
              Collection<Integer> result = elements.stream().collect(collector).join();

              assertThat(result)
                .hasSameSizeAs(elements)
                .containsOnlyElementsOf(elements);
          }),
          dynamicTest("inParallelToCollection should collect to empty", () -> {
              Collection<Integer> result11 = Stream.<Integer>of().collect(collector).join();

              assertThat(result11)
                .isEmpty();
          }));
    }

    private Stream<DynamicTest> forSetCollector(Collector<Integer, List<CompletableFuture<Integer>>, CompletableFuture<Set<Integer>>> collector) {
        return of(
          dynamicTest("inParallelToSet should collect", () -> {
              List<Integer> elements = IntStream.generate(() -> 42).limit(100).boxed().collect(Collectors.toList());
              Collection<Integer> result = elements.stream().collect(collector).join();

              assertThat(result)
                .hasSize(1)
                .contains(42);
          }),
          dynamicTest("inParallelToSet should collect to empty", () -> {
              Collection<Integer> result11 = Stream.<Integer>of().collect(collector).join();

              assertThat(result11)
                .isEmpty();
          }));
    }

    private Stream<DynamicTest> forListCollector(Collector<Integer, List<CompletableFuture<Integer>>, CompletableFuture<List<Integer>>> collector) {
        return of(
          dynamicTest("inParallelToList should collect", () -> {
              List<Integer> elements = IntStream.range(0, 10).boxed().collect(Collectors.toList());
              Collection<Integer> result = elements.stream().collect(collector).join();

              assertThat(result)
                .hasSameSizeAs(elements)
                .containsOnlyElementsOf(elements);
          }),
          dynamicTest("inParallelToList should collect to empty", () -> {
              Collection<Integer> result11 = Stream.<Integer>of().collect(collector).join();

              assertThat(result11)
                .isEmpty();
          }));
    }
}
