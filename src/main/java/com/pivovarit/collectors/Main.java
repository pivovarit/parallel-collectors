package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.supplier;

public class Main {

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Stream.generate(() -> supplier(() -> {
            try {
                Thread.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return 42L;
        }))
          .limit(1)
          .collect(ParallelCollectors.inParallelToCollection(ArrayList::new, executorService, 1))
          .join();
    }
}
