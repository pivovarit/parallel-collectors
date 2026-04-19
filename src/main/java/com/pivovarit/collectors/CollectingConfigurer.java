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

public final class CollectingConfigurer {

    Integer parallelism;
    Executor executor;
    boolean batching;
    Function<Runnable, Runnable> taskDecorator;
    Function<Executor, Executor> executorDecorator;

    CollectingConfigurer() {
    }

    public CollectingConfigurer parallelism(int p) {
        if (p < 1) {
            throw new IllegalArgumentException("parallelism must be greater than 0");
        }
        this.parallelism = p;
        return this;
    }

    public CollectingConfigurer executor(Executor e) {
        this.executor = Objects.requireNonNull(e, "executor can't be null");
        return this;
    }

    public CollectingConfigurer batching() {
        this.batching = true;
        return this;
    }

    public CollectingConfigurer taskDecorator(Function<Runnable, Runnable> decorator) {
        Objects.requireNonNull(decorator, "task decorator can't be null");
        if (this.taskDecorator != null) {
            throw new IllegalArgumentException("task decorator already set");
        }
        this.taskDecorator = decorator;
        return this;
    }

    public CollectingConfigurer executorDecorator(Function<Executor, Executor> o) {
        this.executorDecorator = Objects.requireNonNull(o, "executor decorator can't be null");
        return this;
    }
}
