package com.pivovarit.collectors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
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

        Map<Class<? extends Option>, Integer> counts = new HashMap<>();

        Optional<Boolean> batching = Optional.empty();
        OptionalInt parallelism = OptionalInt.empty();
        Optional<Executor> executor = Optional.empty();

        for (Option option : options) {
            if (counts.get(option.getClass()) != null) {
                throw new IllegalArgumentException("each option can be used at most once, and you configured '%s' multiple times".formatted(switch (option) {
                    case Option.Batching __ -> "batching";
                    case Option.Parallelism __ -> "parallelism";
                    case Option.ThreadPool __ -> "executor";
                }));
            }
            counts.merge(option.getClass(), 1, Integer::sum);

            switch (option) {
                case Batching batchingOption -> batching = Optional.of(batchingOption.batched());
                case Parallelism parallelismOption -> parallelism = OptionalInt.of(parallelismOption.parallelism());
                case ThreadPool threadPoolOption -> executor = Optional.ofNullable(threadPoolOption.executor());
            }
        }

        return new Configuration(batching, parallelism, executor);
    }

    record Batching(boolean batched) implements Option {
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
        return new Batching(true);
    }

    static Option parallelism(int parallelism) {
        return new Parallelism(parallelism);
    }
}
