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
