package com.pivovarit.collectors;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class GroupingSpliteratorTest {

    @Test
    void example_() throws Exception {

        List<Integer> list = Arrays.asList(1, 2, 3, 4);
        Stream<Integer> join = StreamSupport.stream(GroupingSpliterator.partitioned(list, 2), false)
          .collect(AsyncParallelCollector.flattening(i -> i, Executors.newFixedThreadPool(20), 3)).join();

        System.out.println(join.collect(toList()));
    }

    @Test
    void shouldSplitAppropriately() throws Exception {
        List<Integer> collect = Stream.generate(() -> 42).limit(16).collect(Collectors.toList());

        List<List<Integer>> result = StreamSupport.stream(GroupingSpliterator.partitioned(collect, 7), false)
          .collect(toList());

        System.out.println(result);

        assertThat(result)
          .hasSizeBetween(6, 7)
          .allMatch(list -> list.size() < 4 && list.size() > 0);
    }

    @Test
    void shouldGroup() throws Exception {

        List<Integer> list = Arrays.asList(1, 2, 3, 4);

        List<List<Integer>> result = StreamSupport.stream(GroupingSpliterator.partitioned(list, 2), false)
          .collect(toList());

        System.out.println(result);


        assertThat(result)
          .hasSize(2)
          .contains(Arrays.asList(1, 2), Arrays.asList(3, 4));
    }

    @Test
    void shouldPartitionOne() throws Exception {

        List<Integer> list = Arrays.asList(1, 2, 3, 4);

        List<List<Integer>> result = StreamSupport.stream(GroupingSpliterator.partitioned(list, 1), false)
          .collect(toList());

        System.out.println(result);


        assertThat(result)
          .hasSize(1)
          .contains(list);
    }

    @Test
    void shouldGroupEmpty() throws Exception {

        List<Integer> list = Collections.emptyList();

        List<List<Integer>> result = StreamSupport.stream(GroupingSpliterator.partitioned(list, 2), false)
          .collect(toList());
        System.out.println(result);


        assertThat(result)
          .isEmpty();
    }
}