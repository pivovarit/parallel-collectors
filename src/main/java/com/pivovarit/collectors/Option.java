package com.pivovarit.collectors;

import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.Executor;

import static java.util.Objects.requireNonNull;

// while this might seem weird at first, this allows spotting configuration issues at compile-time without duplicating the whole Option hierarchy
sealed interface Option extends StreamingOption {

    record Configuration(Optional<Boolean> ordered, Optional<Boolean> batching, OptionalInt parallelism, Optional<Executor> executor) {
        public Configuration {
            requireNonNull(ordered, "'ordered' can't be null");
            requireNonNull(batching, "'batching' can't be null");
            requireNonNull(parallelism, "'parallelism' can't be null");
            requireNonNull(executor, "'executor' can't be null");
        }
    }

    static Configuration process(StreamingOption... options) {
        requireNonNull(options, "options can't be null");

        Set<Class<? extends StreamingOption>> seen = new HashSet<>();

        Optional<Boolean> batching = Optional.empty();
        Optional<Boolean> ordered = Optional.empty();
        OptionalInt parallelism = OptionalInt.empty();
        Optional<Executor> executor = Optional.empty();

        for (var option : options) {
            if (!seen.add(option.getClass())) {
                throw new IllegalArgumentException("each option can be used at most once, and you configured '%s' multiple times".formatted(switch (option) {
                    case Batching __ -> "batching";
                    case Parallelism __ -> "parallelism";
                    case ThreadPool __ -> "executor";
                    case Ordered __ -> "ordered";
                }));
            }

            switch (option) {
                case Batching __ -> batching = Optional.of(true);
                case Parallelism parallelismOption -> parallelism = OptionalInt.of(parallelismOption.parallelism());
                case ThreadPool threadPoolOption -> executor = Optional.ofNullable(threadPoolOption.executor());
                case Ordered __ -> ordered = Optional.of(true);
            }
        }

        return new Configuration(ordered, batching, parallelism, executor);
    }

    record Ordered() implements StreamingOption {

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

    static ThreadPool executor(Executor executor) {
        return new ThreadPool(executor);
    }

    static Batching batched() {
        return new Batching();
    }

    static Parallelism parallelism(int parallelism) {
        return new Parallelism(parallelism);
    }

    static Ordered ordered() {
        return new Ordered();
    }
}
