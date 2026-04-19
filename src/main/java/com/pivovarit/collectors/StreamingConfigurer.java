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

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Fluent configuration builder for collectors that <em>stream</em> results.
 * <p>
 * Instances of this class are used internally to accumulate configuration options that are later
 * interpreted by {@link ConfigProcessor}.
 */
public final class StreamingConfigurer {

    Integer parallelism;
    Executor executor;
    boolean batching;
    boolean ordered;
    Function<Runnable, Runnable> taskDecorator;
    Function<Executor, Executor> executorDecorator;

    StreamingConfigurer() {
    }

    /**
     * Sets the maximum level of parallelism.
     * <p>
     * This limits the number of tasks submitted to the worker queue at once, effectively bounding
     * the amount of in-flight work and the maximum concurrency used by the collector.
     *
     * @param p the desired parallelism level (must be positive)
     *
     * @return this configurer instance for fluent chaining
     */
    public StreamingConfigurer parallelism(int p) {
        if (p < 1) {
            throw new IllegalArgumentException("parallelism must be greater than 0");
        }
        this.parallelism = p;
        return this;
    }

    /**
     * Sets the {@link Executor} used for running tasks.
     * <p>
     * Use this to supply a custom execution strategy (for example, a dedicated thread pool).
     *
     * <p><b>Note:</b> The provided executor must not <em>drop</em> tasks on rejection (e.g. using a
     * {@code RejectedExecutionHandler} that discards submitted work). Dropping tasks will cause the
     * stream to wait for results that will never be produced, which can lead to deadlocks.
     *
     * @param e the executor to use
     *
     * @return this configurer instance for fluent chaining
     */
    public StreamingConfigurer executor(Executor e) {
        this.executor = Objects.requireNonNull(e, "executor can't be null");
        return this;
    }

    /**
     * Enables batching of work submitted to workers.
     * <p>
     * When enabled, each worker thread receives a batch of input items and processes them in one go,
     * instead of scheduling one task per item. This reduces the number of tasks created and typically
     * decreases contention on the underlying worker queue.
     *
     * <p><b>Note:</b> Depending on batch sizing and workload skew, batching may reduce load balancing and
     * can lead to thread starvation (some workers become idle while others remain overloaded).
     *
     * @return this configurer instance for fluent chaining
     */
    public StreamingConfigurer batching() {
        this.batching = true;
        return this;
    }

    /**
     * Preserves encounter order of the input when emitting results.
     * <p>
     * Enabling this option may reduce throughput compared to unordered streaming, since it can
     * require additional coordination and buffering to emit results in encounter order.
     *
     * @return this configurer instance for fluent chaining
     */
    public StreamingConfigurer ordered() {
        this.ordered = true;
        return this;
    }

    /**
     * Wraps each task with the provided decorator before submission.
     * <p>
     * Useful for propagating thread-local context, adding instrumentation, or applying cross-cutting
     * concerns (e.g. logging, tracing) around the execution of each mapper invocation.
     *
     * @param o function that wraps the original {@code Runnable}
     *
     * @return this configurer instance for fluent chaining
     */
    public StreamingConfigurer taskDecorator(Function<Runnable, Runnable> o) {
        Objects.requireNonNull(o, "task decorator can't be null");
        if (this.taskDecorator != null) {
            throw new IllegalArgumentException("task decorator already set");
        }
        this.taskDecorator = o;
        return this;
    }

    /**
     * Wraps the underlying {@link Executor} with the provided decorator.
     * <p>
     * Useful for instrumenting or augmenting the executor (e.g. with structured concurrency scopes
     * or custom scheduling policies) without replacing it entirely.
     *
     * @param o function that wraps the original {@code Executor}
     *
     * @return this configurer instance for fluent chaining
     */
    public StreamingConfigurer executorDecorator(Function<Executor, Executor> o) {
        this.executorDecorator = Objects.requireNonNull(o, "executor decorator can't be null");
        return this;
    }
}
