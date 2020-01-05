package com.pivovarit.collectors;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Collections;
import java.util.List;

public class Bench {

    @Benchmark
    public void placeholder() {
    }

    public static void main(String[] args) throws RunnerException {
        new Runner(
          new OptionsBuilder()
            .include(Bench.class.getSimpleName())
            .warmupIterations(4)
            .measurementIterations(4)
            .forks(1)
            .build()).run();
    }
}
