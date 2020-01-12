package com.pivovarit.collectors;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
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
      .collect(toList());

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
          .collect(toList());
    }

    @Benchmark
    public List<Integer> parallel_batch_streaming_collect(BenchmarkState state) {
        return source.stream()
          .collect(ParallelCollectors.Batching.parallelToStream(i -> i, state.executor, state.parallelism))
          .collect(toList());
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

/* 972ffbb @ Intel i7-4980HQ (8) @ 2.80GHz, 8u222
Benchmark                               (parallelism)   Mode  Cnt      Score     Error  Units
Bench.parallel_batch_collect                        1  thrpt    5  10218.766 ± 131.633  ops/s
Bench.parallel_batch_collect                       10  thrpt    5   8096.380 ± 197.893  ops/s
Bench.parallel_batch_collect                      100  thrpt    5   2441.829 ± 207.863  ops/s
Bench.parallel_batch_collect                     1000  thrpt    5   1092.730 ±  54.745  ops/s
Bench.parallel_batch_streaming_collect              1  thrpt    5  10715.432 ±  78.383  ops/s
Bench.parallel_batch_streaming_collect             10  thrpt    5   7894.899 ± 229.013  ops/s
Bench.parallel_batch_streaming_collect            100  thrpt    5   3089.166 ± 181.723  ops/s
Bench.parallel_batch_streaming_collect           1000  thrpt    5   1001.557 ±  68.654  ops/s
Bench.parallel_collect                              1  thrpt    5     67.891 ±   1.357  ops/s
Bench.parallel_collect                             10  thrpt    5    105.943 ±   7.940  ops/s
Bench.parallel_collect                            100  thrpt    5    597.102 ±  78.423  ops/s
Bench.parallel_collect                           1000  thrpt    5    948.764 ±  84.439  ops/s
Bench.parallel_streaming                            1  thrpt    5     50.920 ±   1.569  ops/s
Bench.parallel_streaming                           10  thrpt    5     94.064 ±   4.054  ops/s
Bench.parallel_streaming                          100  thrpt    5    619.749 ±  52.431  ops/s
Bench.parallel_streaming                         1000  thrpt    5    998.359 ± 116.077  ops/s
 */
