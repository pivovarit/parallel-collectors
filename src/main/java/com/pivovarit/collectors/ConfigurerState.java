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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

final class ConfigurerState {

    private final List<ConfigProcessor.Option> modifiers = new ArrayList<>();
    private final Set<Class<? extends ConfigProcessor.Option>> seen = new HashSet<>();

    void ordered() {
        addOnce(ConfigProcessor.Option.Ordered.INSTANCE);
    }

    void batching() {
        addOnce(ConfigProcessor.Option.Batched.INSTANCE);
    }

    void parallelism(int parallelism) {
        addOnce(new ConfigProcessor.Option.Parallelism(parallelism));
    }

    void timeout(long duration, TimeUnit unit) {
        Preconditions.requireValidTimeout(duration, unit);

        timeout(Duration.ofNanos(unit.toNanos(duration)));
    }

    void timeout(Duration duration) {
        addOnce(new ConfigProcessor.Option.Timeout(duration));
    }

    void executor(Executor executor) {
        addOnce(new ConfigProcessor.Option.ThreadPool(executor));
    }

    void executorDecorator(UnaryOperator<Executor> decorator) {
        Objects.requireNonNull(decorator, "executor decorator can't be null");

        addOnce(new ConfigProcessor.Option.ExecutorDecorator(decorator));
    }

    void taskDecorator(UnaryOperator<Runnable> decorator) {
        Objects.requireNonNull(decorator, "task decorator can't be null");

        addOnce(new ConfigProcessor.Option.TaskDecorator(decorator));
    }

    List<ConfigProcessor.Option> getConfig() {
        return Collections.unmodifiableList(modifiers);
    }

    void validate() {
        if (seen.contains(ConfigProcessor.Option.Batched.class) && !seen.contains(ConfigProcessor.Option.Parallelism.class)) {
            throw new IllegalStateException("parallelism must be configured when batching is enabled");
        }
    }

    private void addOnce(ConfigProcessor.Option option) {
        if (!seen.add(option.getClass())) {
            throw new IllegalArgumentException("'%s' can only be configured once".formatted(ConfigProcessor.toHumanReadableString(option)));
        }
        modifiers.add(option);
    }
}
