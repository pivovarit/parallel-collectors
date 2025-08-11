package com.pivovarit.collectors;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
            right.build().forEach(left::add);
            return left;
        };
    }

    @Override
    public Function<Stream.Builder<T>, CompletableFuture<RR>> finisher() {
        return acc -> {
            try {
                return CompletableFuture.supplyAsync(() -> processor.apply(acc.build().map(mapper)), executor);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of();
    }
}
