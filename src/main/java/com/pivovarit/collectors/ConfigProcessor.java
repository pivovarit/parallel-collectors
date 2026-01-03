package com.pivovarit.collectors;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Objects.requireNonNull;

final class ConfigProcessor {

    private static final ExecutorService DEFAULT_EXECUTOR = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
      .name("parallel-collectors-", 0)
      .factory());

    record Config(boolean ordered, boolean batching, int parallelism, Executor executor) {
        Config {
            Objects.requireNonNull(executor, "executor can't be null");
        }
    }

    static Config process(Options.CollectingOption... options) {
        requireNonNull(options, "options can't be null");

        Set<Class<? extends Options.CollectingOption>> seen = new HashSet<>();

        Boolean batching = null;
        Boolean ordered = null;
        Integer parallelism = null;
        Executor executor = null;

        for (var option : options) {
            if (!seen.add(option.getClass())) {
                throw new IllegalArgumentException("each option can be used at most once, and you configured '%s' multiple times".formatted(toHumanReadableString(option)));
            }

            switch (option) {
                case Options.Batched __ -> batching = true;
                case Options.Parallelism parallelismOption -> parallelism = parallelismOption.parallelism();
                case Options.ThreadPool threadPoolOption -> executor = threadPoolOption.executor();
                case Options.Ordered __ -> ordered = true;
            }
        }

        return new Config(
          Objects.requireNonNullElse(ordered, false),
          Objects.requireNonNullElse(batching, false),
          Objects.requireNonNullElse(parallelism, 0),
          Objects.requireNonNullElse(executor, DEFAULT_EXECUTOR));
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
