package com.pivovarit.collectors;

import static com.pivovarit.collectors.BatchingSpliterator.partitioned;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BatchingSpliteratorTest {

    @Test
    void shouldSplitInNEvenBatches() {
        var list = IntStream.range(0, 10).boxed().toList();

        var result = partitioned(list, 3).toList();

        assertThat(result)
          .hasSize(3)
          .extracting(List::size)
          .contains(4, 3);
    }

    @Test
    void shouldSplitInNBatches() {
        var list = IntStream.range(0, 10).boxed().toList();

        var result = partitioned(list, 2).toList();

        assertThat(result)
          .hasSize(2)
          .extracting(List::size)
          .containsOnly(5);
    }

    @Test
    void shouldSplitInNSingletonLists() {
        var list = IntStream.range(0, 5).boxed().toList();

        var result = partitioned(list, 10).toList();

        assertThat(result)
          .hasSize(5)
          .extracting(List::size)
          .containsOnly(1);
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
}
