package com.pivovarit.collectors.example;

import com.pivovarit.collectors.Config;
import com.pivovarit.collectors.ParallelCollectors;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.pivovarit.collectors.Config.with;

class Main {

    public static void main(String[] args) {
        List<Integer> ints = List.of(1, 2, 3, 4);

        ExecutorService e = Executors.newCachedThreadPool();

        var result = timed(() -> ints.stream()
          .collect(ParallelCollectors.parallel(
            i -> process(i),
            Collectors.toList(),
            with()
              .executor(e)
              .parallelism(4)
              .batching(false)
              .build())).join());

        System.out.println(result);
    }

    public static <T> T process(T input) {
        System.out.println("Processing " + input + " on " + Thread.currentThread().getName());
        try {
            Thread.sleep(Duration.ofSeconds(1));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return input;
    }

    public static <T> T timed(Supplier<T> supplier) {
        long before = System.currentTimeMillis();
        T result = supplier.get();
        long after = System.currentTimeMillis();

        System.out.println("Time taken: " + Duration.ofMillis(after - before));
        return result;
    }
}
