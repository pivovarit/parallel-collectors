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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

final class ConfigProcessor {

    private ConfigProcessor() {
    }

    static Config fromCollecting(Consumer<CollectingConfigurer> configurer) {
        var cfg = new CollectingConfigurer();
        configurer.accept(cfg);
        validate(cfg.batching, cfg.parallelism);
        return new Config(cfg.parallelism, cfg.executor, cfg.batching, false, cfg.taskDecorator, cfg.executorDecorator);
    }

    static Config fromStreaming(Consumer<StreamingConfigurer> configurer) {
        var cfg = new StreamingConfigurer();
        configurer.accept(cfg);
        validate(cfg.batching, cfg.parallelism);
        return new Config(cfg.parallelism, cfg.executor, cfg.batching, cfg.ordered, cfg.taskDecorator, cfg.executorDecorator);
    }

    static Config collectingWithParallelism(int parallelism) {
        requirePositive(parallelism);
        return new Config(parallelism, null, false, false, null, null);
    }

    static Config streamingWithParallelism(int parallelism) {
        requirePositive(parallelism);
        return new Config(parallelism, null, false, false, null, null);
    }

    static Config empty() {
        return new Config(null, null, false, false, null, null);
    }

    static Executor resolveExecutor(Config config) {
        Executor base = config.executor() != null ? config.executor() : VirtualThreadExecutor.INSTANCE;
        validateExecutor(base);
        if (config.executorDecorator() != null) {
            Executor decorated = config.executorDecorator().apply(base);
            Objects.requireNonNull(decorated, "executor decorator returned null");
            validateExecutor(decorated);
            return decorated;
        }
        return base;
    }

    private static void validate(boolean batching, Integer parallelism) {
        if (batching && parallelism == null) {
            throw new IllegalArgumentException("parallelism must be set when batching is enabled");
        }
    }

    private static void requirePositive(int parallelism) {
        if (parallelism < 1) {
            throw new IllegalArgumentException("parallelism must be greater than 0");
        }
    }

    private static void validateExecutor(Executor e) {
        if (e instanceof ThreadPoolExecutor tpe) {
            var policy = tpe.getRejectedExecutionHandler();
            if (policy instanceof ThreadPoolExecutor.DiscardPolicy
                || policy instanceof ThreadPoolExecutor.DiscardOldestPolicy) {
                throw new IllegalArgumentException("executor can't discard tasks");
            }
        }
    }

    static final class VirtualThreadExecutor implements Executor {
        static final VirtualThreadExecutor INSTANCE = new VirtualThreadExecutor();

        private VirtualThreadExecutor() {
        }

        @Override
        public void execute(Runnable command) {
            Thread.startVirtualThread(command);
        }
    }
}
