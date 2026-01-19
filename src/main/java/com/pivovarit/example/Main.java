package com.pivovarit.example;

import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallel;
import static com.pivovarit.collectors.ParallelCollectors.parallelBy;
import static java.util.stream.Collectors.toList;

class Main {
    public static void main(String[] args) {
        Stream.of(1, 2, 3)
          .collect(parallel(i -> i, c -> c
              .parallelism(2)
              .batching(),
            toList()))
          .join();

        Stream.of(1, 2, 3)
          .collect(parallelBy(i -> i % 2, i -> i, 2, toList()))
          .join();
    }
}
