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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author Grzegorz Piwowarek
 */
sealed abstract class AbstractParallelCollector<T, A, R>
  implements Collector<T, List<CompletableFuture<A>>, R>
  permits AsyncParallelCollector, AsyncParallelStreamingCollector {

    protected final Dispatcher<A> dispatcher;
    protected final Function<? super T, ? extends A> task;

    protected AbstractParallelCollector(Function<? super T, ? extends A> task, Dispatcher<A> dispatcher) {
        this.task = task;
        this.dispatcher = dispatcher;
    }

    abstract Function<List<CompletableFuture<A>>, R> finalizer();

    @Override
    public final Supplier<List<CompletableFuture<A>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public final BiConsumer<List<CompletableFuture<A>>, T> accumulator() {
        return (acc, e) -> {
            if (!dispatcher.wasStarted()) {
                dispatcher.start();
            }
            acc.add(dispatcher.submit(() -> task.apply(e)));
        };
    }

    @Override
    public BinaryOperator<List<CompletableFuture<A>>> combiner() {
        return (left, right) -> {
            throw new UnsupportedOperationException("using parallel stream with parallel collectors is not supported");
        };
    }

    @Override
    public final Function<List<CompletableFuture<A>>, R> finisher() {
        return list -> {
            dispatcher.stop();
            return finalizer().apply(list);
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
