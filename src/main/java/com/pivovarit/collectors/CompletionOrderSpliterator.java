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

import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * @author Grzegorz Piwowarek
 */
final class CompletionOrderSpliterator<T> implements Spliterator<T> {

    private final BlockingQueue<CompletableFuture<T>> completed = new LinkedBlockingQueue<>();
    private final Deadline deadline;
    private int remaining;

    CompletionOrderSpliterator(List<CompletableFuture<T>> futures) {
        this(futures, null);
    }

    CompletionOrderSpliterator(List<CompletableFuture<T>> futures, Deadline deadline) {
        this.remaining = futures.size();
        this.deadline = deadline;
        futures.forEach(f -> f.whenComplete((__, ___) -> completed.add(f)));
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (remaining <= 0) {
            return false;
        }
        action.accept(nextCompleted().join());
        return true;
    }

    private CompletableFuture<T> nextCompleted() {
        try {
            CompletableFuture<T> next = deadline == null
              ? completed.take()
              : completed.poll(deadline.remainingNanos(), TimeUnit.NANOSECONDS);

            if (next == null) {
                throw new CompletionException(new TimeoutException("Timeout while streaming results"));
            }
            remaining--;
            return next;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(e);
        }
    }

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return remaining;
    }

    @Override
    public int characteristics() {
        return IMMUTABLE;
    }
}
