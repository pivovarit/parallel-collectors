package com.pivovarit.collectors;

import java.util.concurrent.Executor;

public record Config(Executor executor, int parallelism, boolean batched) {

    public Config {
        if (parallelism < 0) {
            throw new IllegalArgumentException("parallelism must be non-negative");
        }
    }

    public boolean unlimitedParallelism() {
        return parallelism == 0;
    }

    public static Builder with() {
        return new Builder();
    }

    public static class Builder {
        private Executor executor = null;
        private int parallelism = 0;
        private boolean batched =  false;

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder parallelism(int parallelism) {
            this.parallelism = parallelism;
            return this;
        }

        public Builder batching(boolean batched) {
            this.batched = batched;
            return this;
        }

        public Config build() {
            return new Config(executor, parallelism, batched);
        }
    }
}
