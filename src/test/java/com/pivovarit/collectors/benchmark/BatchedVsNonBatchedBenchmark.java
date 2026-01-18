/*
 * Copyright 2014-2026 Grzegorz Piwowarek, https://4comprehension.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pivovarit.collectors.benchmark;

import com.pivovarit.collectors.ParallelCollectors;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.RunnerException;

import static java.util.stream.Collectors.toList;

public class BatchedVsNonBatchedBenchmark {

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
          .collect(ParallelCollectors.parallel(i -> i, toList(), c -> c.executor(state.executor).parallelism(state.parallelism)))
          .join();
    }

    @Benchmark
    public List<Integer> parallel_batch_collect(BenchmarkState state) {
        return source.stream()
          .collect(ParallelCollectors.parallel(i -> i, toList(), c -> c.batching().executor(state.executor).parallelism(state.parallelism)))
          .join();
    }

    @Benchmark
    public List<Integer> parallel_streaming(BenchmarkState state) {
        return source.stream()
          .collect(ParallelCollectors.parallelToStream(i -> i, c -> c.batching().executor(state.executor).parallelism(state.parallelism)))
          .toList();
    }

    @Benchmark
    public List<Integer> parallel_batch_streaming_collect(BenchmarkState state) {
        return source.stream()
          .collect(ParallelCollectors.parallelToStream(i -> i, c -> c.batching().executor(state.executor).parallelism(state.parallelism)))
          .toList();
    }

    public static void main(String[] args) throws RunnerException {
        Benchmarks.run(BatchedVsNonBatchedBenchmark.class);
    }
}
