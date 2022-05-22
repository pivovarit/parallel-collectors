package com.pivovarit.collectors;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.pivovarit.collectors.BatchingSpliterator.partitioned;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchingSpliteratorTest {

    @Test
    void shouldSplitInNEvenBatches() {
        List<Integer> list = Stream.generate(() -> 42).limit(10).collect(Collectors.toList());

        List<List<Integer>> result = partitioned(list, 3).collect(Collectors.toList());

        assertThat(result)
            .hasSize(3)
            .extracting(List::size)
            .contains(4, 3);
    }

    @Test
    void shouldSplitInNBatches() {
        List<Integer> list = Stream.generate(() -> 42).limit(10).collect(Collectors.toList());

        List<List<Integer>> result = partitioned(list, 2).collect(Collectors.toList());

        assertThat(result)
            .hasSize(2)
            .extracting(List::size)
            .containsOnly(5);
    }

    @Test
    void shouldSplitInNSingletonLists() {
        List<Integer> list = Stream.generate(() -> 42).limit(5).collect(Collectors.toList());

        List<List<Integer>> result = partitioned(list, 10).collect(Collectors.toList());

        assertThat(result)
          .hasSize(5)
          .extracting(List::size)
          .containsOnly(1);
    }

    @Test
    void shouldReturnNestedListIfOneBatch() {
        List<Integer> list = Stream.generate(() -> 42).limit(10).collect(Collectors.toList());

        List<List<Integer>> result = partitioned(list, 1).collect(Collectors.toList());

        assertThat(result.get(0)).containsExactlyElementsOf(list);
    }

    @Test
    void shouldReturnEmptyIfZeroParts() {
        assertThatThrownBy(() -> partitioned(Arrays.asList(1, 2, 3), 0).collect(Collectors.toList()));
    }
}
