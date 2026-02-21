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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.UnaryOperator;

/**
 * Fluent configuration builder for collectors that <em>collect</em> all results (i.e. non-streaming).
 * <p>
 * Instances of this class are used internally to accumulate configuration options that are later
 * interpreted by {@link ConfigProcessor}.
 */
public final class CollectingConfigurer {

    private final List<ConfigProcessor.Option> modifiers = new ArrayList<>();
    private final Set<Class<? extends ConfigProcessor.Option>> seen = new HashSet<>();

    CollectingConfigurer() {
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
        addOnce(ConfigProcessor.Option.Batched.INSTANCE);
        return this;
    }

    /**
     * Sets the maximum level of parallelism.
     * <p>
     * This limits the number of tasks submitted to the worker queue at once, effectively bounding
     * the amount of in-flight work and the maximum concurrency used by the collector.
     *
     * @param parallelism the desired parallelism level (must be positive)
     *
     * @return this configurer instance for fluent chaining
     */
    public CollectingConfigurer parallelism(int parallelism) {
        Preconditions.requireValidParallelism(parallelism);

        addOnce(new ConfigProcessor.Option.Parallelism(parallelism));
        return this;
    }

    /**
     * Sets the {@link Executor} used for running tasks.
     *
     * <p><b>Note:</b> The provided executor must not <em>drop</em> tasks on rejection (e.g. using a
     * {@code RejectedExecutionHandler} that discards submitted work). Dropping tasks will cause the
     * collector to wait for results that will never be produced, which can lead to deadlocks.
     *
     * @param executor the executor to use
     *
     * @return this configurer instance for fluent chaining
     */
    public CollectingConfigurer executor(Executor executor) {
        Preconditions.requireValidExecutor(executor);

        addOnce(new ConfigProcessor.Option.ThreadPool(executor));
        return this;
    }

    /**
     * Decorates the executor used for running tasks.
     * <p>
     * The decorator receives the resolved executor (either the default virtual-thread executor or
     * the one provided via {@link #executor(Executor)}) and returns a wrapped replacement.
     * This is useful for augmenting the executor with cross-cutting concerns such as context
     * propagation (MDC, OpenTelemetry spans, etc.) or monitoring, without replacing the executor entirely.
     *
     * <p><b>Note:</b> The executor returned by the decorator must not <em>drop</em> tasks on rejection.
     * Dropping tasks will cause the collector to wait for results that will never be produced,
     * which can lead to deadlocks.
     *
     * @param decorator a function that wraps the resolved executor
     *
     * @return this configurer instance for fluent chaining
     */
    public CollectingConfigurer executorDecorator(UnaryOperator<Executor> decorator) {
        Objects.requireNonNull(decorator, "executor decorator can't be null");

        addOnce(new ConfigProcessor.Option.ExecutorDecorator(decorator));
        return this;
    }

    /**
     * Decorates each individual task before it is submitted to the executor.
     * <p>
     * The decorator receives the {@link Runnable} representing a single unit of work and returns a
     * wrapped replacement that runs in its place. This is useful for propagating thread-local context
     * (e.g. MDC entries, OpenTelemetry spans, {@code SecurityContext}) into worker threads, or for
     * per-task instrumentation, without replacing the executor entirely.
     *
     * <p>Unlike {@link #executorDecorator(UnaryOperator)}, which wraps the executor as a whole,
     * this decorator is applied to each task individually and runs on the worker thread.
     *
     * @param decorator a function that wraps each submitted task
     *
     * @return this configurer instance for fluent chaining
     */
    public CollectingConfigurer taskDecorator(UnaryOperator<Runnable> decorator) {
        Objects.requireNonNull(decorator, "task decorator can't be null");

        addOnce(new ConfigProcessor.Option.TaskDecorator(decorator));
        return this;
    }

    List<ConfigProcessor.Option> getConfig() {
        return Collections.unmodifiableList(modifiers);
    }

    private void addOnce(ConfigProcessor.Option option) {
        if (!seen.add(option.getClass())) {
            throw new IllegalArgumentException("'%s' can only be configured once".formatted(ConfigProcessor.toHumanReadableString(option)));
        }
        modifiers.add(option);
    }
}
