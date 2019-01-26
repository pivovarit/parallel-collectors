package com.pivovarit.collectors;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.pivovarit.collectors.ParallelCollectors.inParallelToList;

public class Main {

    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(1, 2, 3);
        Executor executor = Executors.newFixedThreadPool(42);

        List<String> result = list.stream()
          .collect(inParallelToList(i -> fetchFromDb(i), executor))
          .join();
    }

    private static String fetchFromDb(Integer i) {
        return "";
    }
}
