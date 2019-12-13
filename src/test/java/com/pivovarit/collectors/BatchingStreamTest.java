package com.pivovarit.collectors;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BatchingStreamTest {

    @Test
    void shouldSplitInNBatches() throws Exception {
        List<Integer> list = Stream.generate(() -> 42).limit(10).collect(Collectors.toList());

        List<List<Integer>> result = BatchingStream.partitioned(list, 2).collect(Collectors.toList());

        assertThat(result)
          .hasSize(2)
          .extracting(List::size)
          .containsOnly(5);
    }
}
