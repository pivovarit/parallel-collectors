package com.pivovarit.collectors.benchmark;

import com.pivovarit.collectors.ParallelCollectors;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.results.format.ResultFormatType;
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

        @Param({"1", "10", "100", "1000"})
        public int parallelism;

        private volatile ExecutorService executor;

        @Setup(Level.Trial)
        public void setup() {
            executor = Executors.newFixedThreadPool(1000);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            executor.shutdown();
        }
    }

    private static final List<Integer> source = IntStream.range(0, 1000)
      .boxed()
      .toList();

    @Benchmark
    public List<Integer> parallel_collect(BenchmarkState state) {
        return source.stream()
          .collect(ParallelCollectors.parallel(i -> i, toList(), state.executor, state.parallelism))
          .join();
    }

    @Benchmark
    public List<Integer> parallel_batch_collect(BenchmarkState state) {
        return source.stream()
          .collect(ParallelCollectors.Batching.parallel(i -> i, toList(), state.executor, state.parallelism))
          .join();
    }

    @Benchmark
    public List<Integer> parallel_streaming(BenchmarkState state) {
        return source.stream()
          .collect(ParallelCollectors.parallelToStream(i -> i, state.executor, state.parallelism))
          .toList();
    }

    @Benchmark
    public List<Integer> parallel_batch_streaming_collect(BenchmarkState state) {
        return source.stream()
          .collect(ParallelCollectors.Batching.parallelToStream(i -> i, state.executor, state.parallelism))
          .toList();
    }

    public static void main(String[] args) throws RunnerException {
        new Runner(
          new OptionsBuilder()
            .include(Bench.class.getSimpleName())
            .warmupIterations(5)
            .measurementIterations(5)
            .resultFormat(ResultFormatType.JSON)
            .result("result.json")
            .forks(1)
            .build()).run();
    }
}
