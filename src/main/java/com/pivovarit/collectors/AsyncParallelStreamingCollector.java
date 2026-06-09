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
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.pivovarit.collectors.BatchingSpliterator.batching;
import static com.pivovarit.collectors.BatchingSpliterator.partitioned;
import static java.util.Collections.emptySet;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncParallelStreamingCollector<T, R> extends AbstractParallelCollector<T, R, Stream<R>> {

    private static final EnumSet<Characteristics> UNORDERED_CHARACTERISTICS = EnumSet.of(Characteristics.UNORDERED);

    private final CompletionStrategy completionStrategy;
    private final Duration timeout;

    AsyncParallelStreamingCollector(Function<? super T, ? extends R> task, Dispatcher<R> dispatcher, boolean ordered, Duration timeout) {
        super(task, dispatcher);
        this.completionStrategy = ordered ? CompletionStrategy.ORDERED : CompletionStrategy.UNORDERED;
        this.timeout = timeout;
    }

    @Override
    public Function<List<CompletableFuture<R>>, Stream<R>> finalizer() {
        return acc -> switch (completionStrategy) {
            case ORDERED -> orderedStream(acc);
            case UNORDERED -> StreamSupport.stream(
              new CompletionOrderSpliterator<>(acc, timeout == null ? null : new Deadline(timeout.toNanos())), false);
        };
    }

    private Stream<R> orderedStream(List<CompletableFuture<R>> acc) {
        if (timeout == null) {
            return acc.stream().map(CompletableFuture::join).filter(__ -> true);
        }
        var deadline = new Deadline(timeout.toNanos());
        return acc.stream().map(future -> joinWithin(future, deadline)).filter(__ -> true);
    }

    private static <R> R joinWithin(CompletableFuture<R> future, Deadline deadline) {
        try {
            return future.get(deadline.remainingNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            throw new CompletionException(new TimeoutException("Timeout while streaming results").initCause(e));
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        }
    }

    @Override
    public Set<Characteristics> characteristics() {
        return switch (completionStrategy) {
            case ORDERED -> emptySet();
            case UNORDERED -> UNORDERED_CHARACTERISTICS;
        };
    }

    record BatchingCollector<T, R>(Function<? super T, ? extends R> task, Executor executor, int parallelism, boolean ordered, Duration timeout)
      implements Collector<T, ArrayList<T>, Stream<R>> {

        @Override
        public Supplier<ArrayList<T>> supplier() {
            return ArrayList::new;
        }

        @Override
        public BiConsumer<ArrayList<T>, T> accumulator() {
            return ArrayList::add;
        }

        @Override
        public BinaryOperator<ArrayList<T>> combiner() {
            return (left, right) -> {
                throw new UnsupportedOperationException("using parallel stream with parallel collectors is not supported");
            };
        }

        @Override
        public Function<ArrayList<T>, Stream<R>> finisher() {
            return items -> {
                if (items.size() == parallelism) {
                    return items.stream()
                      .collect(new AsyncParallelStreamingCollector<>(task, new Dispatcher<>(executor, parallelism), ordered, timeout));
                } else {
                    return partitioned(items, parallelism)
                      .collect(new AsyncParallelStreamingCollector<>(batching(task), new Dispatcher<>(executor, parallelism), ordered, timeout))
                      .flatMap(Collection::stream);
                }
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            return emptySet();
        }
    }
}
