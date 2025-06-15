package com.pivovarit.collectors;

import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.Executor;

import static java.util.Objects.requireNonNull;

sealed interface Option {

    record Configuration(Optional<Boolean> batching, OptionalInt parallelism, Optional<Executor> executor) {
        public Configuration {
            requireNonNull(batching, "batching can't be null");
            requireNonNull(parallelism, "parallelism can't be null");
            requireNonNull(executor, "executor can't be null");
        }
    }

    static Configuration process(Option... options) {
        requireNonNull(options, "options can't be null");

        Set<Class<? extends Option>> seen = new HashSet<>();

        Optional<Boolean> batching = Optional.empty();
        OptionalInt parallelism = OptionalInt.empty();
        Optional<Executor> executor = Optional.empty();

        for (var option : options) {
            if (!seen.add(option.getClass())) {
                throw new IllegalArgumentException("each option can be used at most once, and you configured '%s' multiple times".formatted(switch (option) {
                    case Option.Batching __ -> "batching";
                    case Option.Parallelism __ -> "parallelism";
                    case Option.ThreadPool __ -> "executor";
                }));
            }

            switch (option) {
                case Batching __ -> batching = Optional.of(true);
                case Parallelism parallelismOption -> parallelism = OptionalInt.of(parallelismOption.parallelism());
                case ThreadPool threadPoolOption -> executor = Optional.ofNullable(threadPoolOption.executor());
            }
        }

        return new Configuration(batching, parallelism, executor);
    }

    record Batching() implements Option {
    }

    record ThreadPool(Executor executor) implements Option {
        public ThreadPool {
            Preconditions.requireValidExecutor(executor);
        }
    }

    record Parallelism(int parallelism) implements Option {
        public Parallelism {
            Preconditions.requireValidParallelism(parallelism);
        }
    }

    static Option executor(Executor executor) {
        return new ThreadPool(executor);
    }

    static Option batched() {
        return new Batching();
    }

    static Option parallelism(int parallelism) {
        return new Parallelism(parallelism);
    }
}
