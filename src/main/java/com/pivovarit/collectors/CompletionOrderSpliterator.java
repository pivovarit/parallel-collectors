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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

final class CompletionOrderSpliterator<R> implements Spliterator<R> {

    private final BlockingQueue<CompletableFuture<R>> completed = new LinkedBlockingQueue<>();
    private int remaining;

    CompletionOrderSpliterator(List<CompletableFuture<R>> futures) {
        this.remaining = futures.size();
        for (CompletableFuture<R> f : futures) {
            f.whenComplete((v, e) -> completed.offer(f));
        }
    }

    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
        while (remaining > 0) {
            CompletableFuture<R> next;
            try {
                next = completed.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
            remaining--;
            try {
                R value = next.join();
                action.accept(value);
                return true;
            } catch (CancellationException ce) {
                continue;
            } catch (CompletionException ce) {
                if (ce.getCause() instanceof CancellationException) {
                    continue;
                }
                throw ce;
            }
        }
        return false;
    }

    @Override
    public Spliterator<R> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return remaining;
    }

    @Override
    public int characteristics() {
        return NONNULL;
    }
}
