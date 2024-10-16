package com.pivovarit.collectors.benchmark;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.file.Path;

final class Benchmarks {

    private Benchmarks() {
    }

    private static final Path BENCHMARKS_PATH = Path.of("src/test/resources/benchmarks/");

    static void run(Class<?> clazz) throws RunnerException {
        new Runner(new OptionsBuilder()
          .include(clazz.getSimpleName())
          .warmupIterations(3)
          .measurementIterations(5)
          .resultFormat(ResultFormatType.JSON)
          .result(Benchmarks.BENCHMARKS_PATH.resolve("%s.json".formatted(clazz.getSimpleName())).toString())
          .forks(1)
          .build()).run();
    }
}
