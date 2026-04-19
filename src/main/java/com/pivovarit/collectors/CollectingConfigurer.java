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
 * Fluent configuration builder for collectors that <em>collect</em> all results (i.e. non-streaming).
 * <p>
 * Instances of this class are used internally to accumulate configuration options that are later
 * interpreted by {@link ConfigProcessor}.
 */
public final class CollectingConfigurer {

    Integer parallelism;
    Executor executor;
    boolean batching;
    Function<Runnable, Runnable> taskDecorator;
    Function<Executor, Executor> executorDecorator;

    CollectingConfigurer() {
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
    public CollectingConfigurer parallelism(int p) {
        if (p < 1) {
            throw new IllegalArgumentException("parallelism must be greater than 0");
        }
        this.parallelism = p;
        return this;
    }

    /**
     * Sets the {@link Executor} used for running tasks.
     *
     * <p><b>Note:</b> The provided executor must not <em>drop</em> tasks on rejection (e.g. using a
     * {@code RejectedExecutionHandler} that discards submitted work). Dropping tasks will cause the
     * collector to wait for results that will never be produced, which can lead to deadlocks.
     *
     * @param e the executor to use
     *
     * @return this configurer instance for fluent chaining
     */
    public CollectingConfigurer executor(Executor e) {
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
    public CollectingConfigurer batching() {
        this.batching = true;
        return this;
    }

    /**
     * Wraps each task with the provided decorator before submission.
     * <p>
     * Useful for propagating thread-local context, adding instrumentation, or applying cross-cutting
     * concerns (e.g. logging, tracing) around the execution of each mapper invocation.
     *
     * @param decorator function that wraps the original {@code Runnable}
     *
     * @return this configurer instance for fluent chaining
     */
    public CollectingConfigurer taskDecorator(Function<Runnable, Runnable> decorator) {
        Objects.requireNonNull(decorator, "task decorator can't be null");
        if (this.taskDecorator != null) {
            throw new IllegalArgumentException("task decorator already set");
        }
        this.taskDecorator = decorator;
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
    public CollectingConfigurer executorDecorator(Function<Executor, Executor> o) {
        this.executorDecorator = Objects.requireNonNull(o, "executor decorator can't be null");
        return this;
    }
}
