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
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

record SyncCollector<T, R>(Function<? super T, ? extends R> mapper)
  implements Collector<T, Stream.Builder<R>, Stream<R>> {

    @Override
    public Supplier<Stream.Builder<R>> supplier() {
        return Stream::builder;
    }

    @Override
    public BiConsumer<Stream.Builder<R>, T> accumulator() {
        return (rs, t) -> rs.add(mapper.apply(t));
    }

    @Override
    public BinaryOperator<Stream.Builder<R>> combiner() {
        return (rs, rs2) -> {
            throw new UnsupportedOperationException("Using parallel stream with parallel collectors is a bad idea");
        };
    }

    @Override
    public Function<Stream.Builder<R>, Stream<R>> finisher() {
        return Stream.Builder::build;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of();
    }
}
