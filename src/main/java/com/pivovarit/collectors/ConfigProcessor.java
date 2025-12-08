package com.pivovarit.collectors;

import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.Executor;

import static java.util.Objects.requireNonNull;

final class ConfigProcessor {

    record Configuration(
      Optional<Boolean> ordered,
      Optional<Boolean> batching,
      OptionalInt parallelism,
      Optional<Executor> executor) {
        public Configuration {
            requireNonNull(ordered, "'ordered' can't be null");
            requireNonNull(batching, "'batching' can't be null");
            requireNonNull(parallelism, "'parallelism' can't be null");
            requireNonNull(executor, "'executor' can't be null");
        }
    }

    static ConfigProcessor.Configuration process(Options.CollectingOption... options) {
        requireNonNull(options, "options can't be null");

        Set<Class<? extends Options.CollectingOption>> seen = new HashSet<>();

        Optional<Boolean> batching = Optional.empty();
        Optional<Boolean> ordered = Optional.empty();
        OptionalInt parallelism = OptionalInt.empty();
        Optional<Executor> executor = Optional.empty();

        for (var option : options) {
            if (!seen.add(option.getClass())) {
                throw new IllegalArgumentException("each option can be used at most once, and you configured '%s' multiple times".formatted(toHumanReadableString(option)));
            }

            switch (option) {
                case Options.Batched __ -> batching = Optional.of(true);
                case Options.Parallelism parallelismOption ->
                  parallelism = OptionalInt.of(parallelismOption.parallelism());
                case Options.ThreadPool threadPoolOption -> executor = Optional.ofNullable(threadPoolOption.executor());
                case Options.Ordered __ -> ordered = Optional.of(true);
            }
        }

        return new Configuration(ordered, batching, parallelism, executor);
    }

    private static String toHumanReadableString(Options.CollectingOption option) {
        return switch (option) {
            case Options.Batched __ -> "batching";
            case Options.Parallelism __ -> "parallelism";
            case Options.ThreadPool __ -> "executor";
            case Options.Ordered __ -> "ordered";
        };
    }
}
