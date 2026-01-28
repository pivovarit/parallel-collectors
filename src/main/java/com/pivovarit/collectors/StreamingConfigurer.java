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
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Fluent configuration builder for collectors that <em>stream</em> results.
 * <p>
 * Instances of this class are used internally to accumulate configuration options that are later
 * interpreted by {@link ConfigProcessor}.
 */
public final class StreamingConfigurer {

    private final List<ConfigProcessor.Option> modifiers = new ArrayList<>();

    /**
     * Creates a new configurer instance.
     */
    public StreamingConfigurer() {
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
        modifiers.add(ConfigProcessor.Option.Ordered.INSTANCE);
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
        modifiers.add(ConfigProcessor.Option.Batched.INSTANCE);
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
    public StreamingConfigurer parallelism(int parallelism) {
        Preconditions.requireValidParallelism(parallelism);

        modifiers.add(new ConfigProcessor.Option.Parallelism(parallelism));
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
     * @param executor the executor to use
     *
     * @return this configurer instance for fluent chaining
     */
    public StreamingConfigurer executor(Executor executor) {
        Preconditions.requireValidExecutor(executor);

        modifiers.add(new ConfigProcessor.Option.ThreadPool(executor));
        return this;
    }

    List<ConfigProcessor.Option> getConfig() {
        return Collections.unmodifiableList(modifiers);
    }
}
