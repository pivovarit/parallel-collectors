package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

import static com.pivovarit.collectors.BatchingSpliterator.partitioned;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;

class BatchingSpliteratorTest {

  @Test
  void shouldReturnEmptyStreamForEmptyList() {
    assertThat(BatchingSpliterator.partitioned(List.of(), 3).toList()).isEmpty();
  }

  @Test
  void shouldSplitAndProcessIndependently() {
    var input = List.of(1, 2, 3, 4, 5, 6, 7, 8);
    var original = new BatchingSpliterator<>(input, 4);

    var split = original.trySplit();
    assertThat(split).isNotNull();

    var splitChunks = StreamSupport.stream(split, false).toList();
    var remainingChunks = StreamSupport.stream(original, false).toList();

    var combined =
        Stream.concat(splitChunks.stream(), remainingChunks.stream())
            .flatMap(List::stream)
            .toList();

    assertThat(combined).containsExactlyElementsOf(input);
    assertThat(splitChunks.size()).isEqualTo(2);
    assertThat(remainingChunks.size()).isEqualTo(2);
  }

  @Test
  void shouldHandleEmptyList() {
    var input = List.of();
    var spliterator = new BatchingSpliterator<>(input, 3);

    assertThat(spliterator.trySplit()).isNull();

    var result = StreamSupport.stream(spliterator, false).toList();

    assertThat(result).isEmpty();
  }

  @Test
  void shouldHandleSingleElementList() {
    var input = List.of(42);
    var spliterator = new BatchingSpliterator<>(input, 3);

    assertThat(spliterator.trySplit()).isNull();

    var result = StreamSupport.stream(spliterator, false).toList();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst()).containsExactly(42);
  }

  @Test
  void shouldSplitInNEvenBatches() {
    var list = IntStream.range(0, 10).boxed().toList();

    var result = partitioned(list, 3).toList();

    assertThat(result).hasSize(3).extracting(List::size).contains(4, 3);
  }

  @Test
  void shouldSplitInNBatches() {
    var list = IntStream.range(0, 10).boxed().toList();

    var result = partitioned(list, 2).toList();

    assertThat(result).hasSize(2).extracting(List::size).containsOnly(5);
  }

  @Test
  void shouldSplitInNSingletonLists() {
    var list = IntStream.range(0, 5).boxed().toList();

    var result = partitioned(list, 10).toList();

    assertThat(result).hasSize(5).extracting(List::size).containsOnly(1);
  }

  @Test
  void shouldReturnNestedListIfOneBatch() {
    var list = IntStream.range(0, 10).boxed().toList();

    var result = partitioned(list, 1).toList();

    assertThat(result.getFirst()).containsExactlyElementsOf(list);
  }

  @Test
  void shouldReturnEmptyIfZeroParts() {
    assertThatThrownBy(() -> partitioned(Arrays.asList(1, 2, 3), 0).toList());
  }

  @Test
  void shouldReportCorrectSizeWhenOneBatch() {
    var list = IntStream.range(0, 10).boxed().toList();

    assertThat(partitioned(list, 1).count()).isEqualTo(1);
  }

  @Test
  void shouldReportCorrectSizeWhenMultipleBatches() {
    var list = IntStream.range(0, 10).boxed().toList();

    assertThat(partitioned(list, 2).count()).isEqualTo(2);
  }

  @Test
  void shouldPartitionEmptyList() {
    List<List<Integer>> result = partitioned(List.<Integer>of(), 3).toList();
    assertThat(result).isEmpty();
  }

  @Test
  void shouldPartitionToSingletonsWhenSizeLessThanBatches() {
    List<List<Integer>> result = partitioned(List.of(1, 2), 5).toList();

    assertThat(result).containsExactly(List.of(1), List.of(2));
  }

  @Test
  void shouldReturnSameListWhenBatchesIsOne() {
    List<Integer> input = List.of(1, 2, 3, 4, 5);
    List<List<Integer>> result = partitioned(input, 1).toList();

    assertThat(result).containsExactly(input);
  }

  @Test
  void shouldNotExceedBoundsForUnevenSplit() {
    List<Integer> input = List.of(1, 2, 3, 4, 5);
    List<List<Integer>> result = partitioned(input, 3).toList();

    assertThat(result).containsExactly(List.of(1, 2), List.of(3, 4), List.of(5));
  }

  @Test
  void shouldNotExceedBoundsOnLargeChunkRecomputation() {
    List<Integer> input = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      input.add(i);
    }

    List<List<Integer>> result = partitioned(input, 3).toList();

    assertThat(result).containsExactly(List.of(0, 1, 2, 3), List.of(4, 5, 6), List.of(7, 8, 9));
  }

  @Test
  void shouldThrowWhenZeroBatches() {
    List<Integer> input = List.of(1, 2, 3);

    assertThrows(IllegalArgumentException.class, () -> partitioned(input, 0));
  }

  @Test
  void shouldHandleExactDivisibility() {
    List<Integer> input = List.of(1, 2, 3, 4);
    List<List<Integer>> result = partitioned(input, 2).toList();

    assertThat(result).containsExactly(List.of(1, 2), List.of(3, 4));
  }

  @Test
  void shouldNotModifyInputList() {
    List<Integer> input = new ArrayList<>(List.of(1, 2, 3, 4, 5));
    partitioned(input, 2).forEach(c -> {});

    assertThat(input).isEqualTo(List.of(1, 2, 3, 4, 5));
  }

  @Test
  void shouldNotOvershootConsumedOnLastBatch() {
    List<Integer> source = List.of(1, 2, 3, 4, 5);

    Spliterator<List<Integer>> spliterator = new BatchingSpliterator<>(source, 2);

    List<List<Integer>> result = new ArrayList<>();
    while (spliterator.tryAdvance(result::add)) {
      // no-op
    }

    assertThat(result).containsExactly(List.of(1, 2, 3), List.of(4, 5));

    assertThat(result.stream().mapToInt(List::size).sum()).isEqualTo(source.size());
  }

  @Test
  void shouldReportOrderedSizedAndSubSized() {
    Spliterator<List<Integer>> spliterator = new BatchingSpliterator<>(List.of(1, 2, 3), 2);

    int characteristics = spliterator.characteristics();

    assertThat(characteristics & Spliterator.IMMUTABLE).isNotZero();
    assertThat(characteristics & Spliterator.ORDERED).isNotZero();
    assertThat(characteristics & Spliterator.SIZED).isNotZero();
    assertThat(characteristics & Spliterator.SUBSIZED).isNotZero();
  }
}
