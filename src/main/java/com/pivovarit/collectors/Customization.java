package com.pivovarit.collectors;

import java.util.Objects;
import java.util.concurrent.Executor;

public sealed interface Customization
  permits Batching, ThreadPool, Parallelism {

    static Customization executor(Executor executor) {
        Objects.requireNonNull(executor);
        return new ThreadPool(executor);
    }

    static Customization batched() {
        return new Batching(true);
    }

    static Customization parallelism(int parallelism) {
        return new Parallelism(parallelism);
    }
}
