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

import java.nio.file.Path;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

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
