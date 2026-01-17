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

final class Preconditions {

    private Preconditions() {
    }

    static void requireValidParallelism(int parallelism) {
        if (parallelism < 1) {
            throw new IllegalArgumentException("Parallelism can't be lower than 1");
        }
    }

    static void requireValidExecutor(Executor executor) {
        Objects.requireNonNull(executor, "Executor can't be null");
        if (executor instanceof ThreadPoolExecutor tpe) {
            switch (tpe.getRejectedExecutionHandler()) {
                case ThreadPoolExecutor.DiscardPolicy ignored ->
                  throw new IllegalArgumentException("Executor's RejectedExecutionHandler can't discard tasks");
                case ThreadPoolExecutor.DiscardOldestPolicy ignored ->
                  throw new IllegalArgumentException("Executor's RejectedExecutionHandler can't discard tasks");
                default -> {
                    // no-op
                }
            }
        }
    }
}
