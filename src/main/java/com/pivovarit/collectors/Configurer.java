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
import java.util.function.Consumer;

sealed interface Configurer {

    List<ConfigProcessor.Option> getConfig();

    static Streaming streaming(Consumer<Streaming> consumer) {
        var c = new Streaming();
        consumer.accept(c);
        return c;
    }

    static Collecting collecting(Consumer<Collecting> consumer) {
        var c = new Collecting();
        consumer.accept(c);
        return c;
    }

    final class Streaming implements Configurer {

        private final List<ConfigProcessor.Option> modifiers = new ArrayList<>();

        public Streaming ordered() {
            modifiers.add(ConfigProcessor.Option.Ordered.INSTANCE);
            return this;
        }

        public Streaming batching() {
            modifiers.add(ConfigProcessor.Option.Batched.INSTANCE);
            return this;
        }

        public Streaming parallelism(int parallelism) {
            modifiers.add(new ConfigProcessor.Option.Parallelism(parallelism));
            return this;
        }

        public Streaming executor(Executor executor) {
            modifiers.add(new ConfigProcessor.Option.ThreadPool(executor));
            return this;
        }

        @Override
        public List<ConfigProcessor.Option> getConfig() {
            return Collections.unmodifiableList(modifiers);
        }
    }

    final class Collecting implements Configurer {

        private final List<ConfigProcessor.Option> modifiers = new ArrayList<>();

        public Collecting batching() {
            modifiers.add(ConfigProcessor.Option.Batched.INSTANCE);
            return this;
        }

        public Collecting parallelism(int parallelism) {
            modifiers.add(new ConfigProcessor.Option.Parallelism(parallelism));
            return this;
        }

        public Collecting executor(Executor executor) {
            modifiers.add(new ConfigProcessor.Option.ThreadPool(executor));
            return this;
        }

        @Override
        public List<ConfigProcessor.Option> getConfig() {
            return modifiers;
        }
    }
}
