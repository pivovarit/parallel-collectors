package com.pivovarit.collectors;

import java.util.Objects;
import java.util.concurrent.Executor;

public sealed interface Modification
  permits Batching, ThreadPool, Parallelism {

    static Modification executor(Executor executor) {
        Objects.requireNonNull(executor);
        return new ThreadPool(executor);
    }

    static Modification batched() {
        return new Batching(true);
    }

    static Modification parallelism(int parallelism) {
        return new Parallelism(parallelism);
    }
}
