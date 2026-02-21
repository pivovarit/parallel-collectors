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
package com.pivovarit.collectors;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;

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

    static Config process(List<Option> options) {
        requireNonNull(options, "options can't be null");

        Boolean batching = null;
        Boolean ordered = null;
        Integer parallelism = null;
        Executor executor = null;
        UnaryOperator<Executor> decorator = null;

        for (var option : options) {
            switch (option) {
                case Option.Batched ignored -> batching = true;
                case Option.Parallelism(var p) -> parallelism = p;
                case Option.ThreadPool(var e) -> executor = e;
                case Option.Ordered ignored -> ordered = true;
                case Option.ExecutorDecorator(var d) -> decorator = d;
            }
        }

        var resolvedExecutor = Objects.requireNonNullElse(executor, DEFAULT_EXECUTOR);
        if (decorator != null) {
            resolvedExecutor = decorator.apply(resolvedExecutor);
            Preconditions.requireValidExecutor(resolvedExecutor);
        }

        return new Config(
          Objects.requireNonNullElse(ordered, false),
          Objects.requireNonNullElse(batching, false),
          Objects.requireNonNullElse(parallelism, 0),
          resolvedExecutor);
    }

    static String toHumanReadableString(Option option) {
        return switch (option) {
            case Option.Batched ignored -> "batching";
            case Option.Parallelism ignored -> "parallelism";
            case Option.ThreadPool ignored -> "executor";
            case Option.Ordered ignored -> "ordered";
            case Option.ExecutorDecorator ignored -> "executor decorator";
        };
    }

    public sealed interface Option {

        enum Ordered implements Option {
            INSTANCE
        }

        enum Batched implements Option {
            INSTANCE
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

        record ExecutorDecorator(UnaryOperator<Executor> decorator) implements Option {
            public ExecutorDecorator {
                Objects.requireNonNull(decorator, "decorator can't be null");
            }
        }
    }
}
