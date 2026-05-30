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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;

/**
 * A {@link CompletableFuture} whose cancellation propagates to the {@link FutureTask} that completes
 * it, so that {@code cancel(true)} interrupts the thread running the backing task.
 */
final class InterruptibleCompletableFuture<T> extends CompletableFuture<T> {

    private volatile FutureTask<?> backingTask;

    void completedBy(FutureTask<?> task) {
        backingTask = task;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (backingTask != null) {
            backingTask.cancel(mayInterruptIfRunning);
        }
        return super.cancel(mayInterruptIfRunning);
    }
}
