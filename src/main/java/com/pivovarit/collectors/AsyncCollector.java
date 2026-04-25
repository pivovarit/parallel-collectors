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

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

record AsyncCollector<T, R, RR>(Function<? super T, ? extends R> mapper, Function<Stream<R>, RR> processor, Executor executor)
  implements Collector<T, Stream.Builder<T>, CompletableFuture<RR>> {

    @Override
    public Supplier<Stream.Builder<T>> supplier() {
        return Stream::builder;
    }

    @Override
    public BiConsumer<Stream.Builder<T>, T> accumulator() {
        return Stream.Builder::add;
    }

    @Override
    public BinaryOperator<Stream.Builder<T>> combiner() {
        return (left, right) -> {
            throw new UnsupportedOperationException("using parallel stream with parallel collectors is not supported");
        };
    }

    @Override
    public Function<Stream.Builder<T>, CompletableFuture<RR>> finisher() {
        return acc -> {
            var result = new InterruptibleCompletableFuture<RR>();
            var task = new FutureTask<>(() -> {
                try {
                    var mapped = acc.build().map(mapper).map(e -> (R) e).toList();
                    result.complete(processor.apply(mapped.stream()));
                } catch (Throwable e) {
                    result.completeExceptionally(e);
                }
            }, null);
            result.completedBy(task);
            result.whenComplete((__, ex) -> {
                if (ex != null) {
                    task.cancel(true);
                }
            });
            try {
                executor.execute(task);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
            return result;
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of();
    }

    private static final class InterruptibleCompletableFuture<T> extends CompletableFuture<T> {

        private volatile FutureTask<?> backingTask;

        private void completedBy(FutureTask<?> task) {
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
}
