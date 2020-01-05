package com.pivovarit.collectors;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class Bench {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        private volatile ExecutorService executor;

        @Setup(Level.Trial)
        public void setup() {
            executor = Executors.newFixedThreadPool(100);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            executor.shutdown();
        }
    }

    private static final List<Integer> source = IntStream.range(0, 100)
      .boxed()
      .collect(toList());

    @Benchmark
    public List<Integer> parallel(BenchmarkState state) {
        return source.stream()
          .collect(ParallelCollectors.parallel(i -> i, toList(), state.executor, 100))
          .join();
    }

    @Benchmark
    public List<Integer> parallel_batching(BenchmarkState state) {
        return source.stream()
          .collect(ParallelCollectors.Batching.parallel(i -> i, toList(), state.executor, 100))
          .join();
    }

    public static void main(String[] args) throws RunnerException {
        new Runner(
          new OptionsBuilder()
            .include(Bench.class.getSimpleName())
            .warmupIterations(5)
            .measurementIterations(5)
            .forks(1)
            .build()).run();
    }
}
