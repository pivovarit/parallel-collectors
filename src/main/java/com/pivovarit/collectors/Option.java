package com.pivovarit.collectors;

import java.util.Objects;
import java.util.concurrent.Executor;

public sealed interface Option
  permits Batching, ThreadPool, Parallelism {

    static Option executor(Executor executor) {
        Objects.requireNonNull(executor);
        return new ThreadPool(executor);
    }

    static Option batched() {
        return new Batching(true);
    }

    static Option parallelism(int parallelism) {
        return new Parallelism(parallelism);
    }
}
