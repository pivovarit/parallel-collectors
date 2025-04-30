package com.pivovarit.collectors.test;

import com.pivovarit.collectors.Config;
import com.pivovarit.collectors.ParallelCollectors;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.test.Factory.GenericCollector.advancedCollector;
import static com.pivovarit.collectors.test.Factory.GenericCollector.collector;
import static com.pivovarit.collectors.test.Factory.GenericCollector.limitedCollector;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

final class Factory {

    private Factory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static Stream<Factory.GenericCollector<Factory.CollectorFactory<Integer, Integer>>> all() {
        return Stream.of(
          collector("parallel(p) via config", f -> collectingAndThen(ParallelCollectors.parallel(f, Config.with().parallelism(p()).build()), c -> c.join().toList())),
          collector("parallel(e) via config", f -> collectingAndThen(ParallelCollectors.parallel(f, Config.with().executor(e()).build()), c -> c.join().toList())),
          collector("parallel(e, p) via config", f -> collectingAndThen(ParallelCollectors.parallel(f, Config.with().executor(e()).parallelism(p()).build()), c -> c.join().toList())),
          collector("parallel(toList(), e, p) [batching] via config", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), Config.with().batching(true).executor(e()).parallelism(p()).build()), CompletableFuture::join)),
          collector("parallel(toList(), p) via config", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), Config.with().parallelism(p()).build()), CompletableFuture::join)),
          collector("parallel(toList(), e) via config", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), Config.with().executor(e()).build()), CompletableFuture::join)),
          collector("parallel(toList(), e, p) via config", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), Config.with().executor(e()).parallelism(p()).build()), CompletableFuture::join)),
          collector("parallelToStream(p) via config", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, Config.with().parallelism(p()).build()), Stream::toList)),
          collector("parallelToStream(e) via config", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, Config.with().executor(e()).build()), Stream::toList)),
          collector("parallelToStream(e, p) via config", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, Config.with().executor(e()).parallelism(p()).build()), Stream::toList)),
          collector("parallelToStream(e, p) [batching] via config", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, Config.with().batching(true).executor(e()).parallelism(p()).build()), Stream::toList)),
          collector("parallelToOrderedStream(p) via config", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, Config.with().parallelism(p()).build()), Stream::toList)),
          collector("parallelToOrderedStream(e) via config", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, Config.with().executor(e()).build()), Stream::toList)),
          collector("parallelToOrderedStream(e, p) via config", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, Config.with().executor(e()).parallelism(p()).build()), Stream::toList)),
          collector("parallelToOrderedStream(e, p) [batching] via config", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, Config.with().batching(true).executor(e()).parallelism(p()).build()), Stream::toList)),
          //
          collector("parallel()", f -> collectingAndThen(ParallelCollectors.parallel(f), c -> c.join().toList())),
          collector("parallel(p)", f -> collectingAndThen(ParallelCollectors.parallel(f, p()), c -> c.join().toList())),
          collector("parallel(e)", f -> collectingAndThen(ParallelCollectors.parallel(f, e()), c -> c.join().toList())),
          collector("parallel(e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, e(), p()), c -> c.join().toList())),
          //
          collector("parallel(toList())", f -> collectingAndThen(ParallelCollectors.parallel(f, toList()), CompletableFuture::join)),
          collector("parallel(toList(), p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e()), CompletableFuture::join)),
          collector("parallel(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          //
          collector("parallelToStream()", f -> collectingAndThen(ParallelCollectors.parallelToStream(f), Stream::toList)),
          collector("parallelToStream(p)", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, p()), Stream::toList)),
          collector("parallelToStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, e()), Stream::toList)),
          collector("parallelToStream(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, e(), p()), Stream::toList)),
          collector("parallelToStream(e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e(), p()), Stream::toList)),
          //
          collector("parallelToOrderedStream()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f), Stream::toList)),
          collector("parallelToOrderedStream(p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, p()), Stream::toList)),
          collector("parallelToOrderedStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e()), Stream::toList)),
          collector("parallelToOrderedStream(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStream(e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p()), Stream::toList))
        );
    }

    static Stream<Factory.GenericCollector<Factory.CollectorFactory<Integer, Integer>>> allOrdered() {
        return Stream.of(
          collector("parallel(p) via config", f -> collectingAndThen(ParallelCollectors.parallel(f, Config.with().parallelism(p()).build()), c -> c.join().toList())),
          collector("parallel(e) via config", f -> collectingAndThen(ParallelCollectors.parallel(f, Config.with().executor(e()).build()), c -> c.join().toList())),
          collector("parallel(e, p) via config", f -> collectingAndThen(ParallelCollectors.parallel(f, Config.with().executor(e()).parallelism(p()).build()), c -> c.join().toList())),
          collector("parallel(toList(), e, p) [batching] via config", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), Config.with().batching(true).executor(e()).parallelism(p()).build()), CompletableFuture::join)),
          collector("parallel(toList(), p) via config", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), Config.with().parallelism(p()).build()), CompletableFuture::join)),
          collector("parallel(toList(), e) via config", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), Config.with().executor(e()).build()), CompletableFuture::join)),
          collector("parallel(toList(), e, p) via config", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), Config.with().executor(e()).parallelism(p()).build()), CompletableFuture::join)),
          collector("parallelToOrderedStream(p) via config", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, Config.with().parallelism(p()).build()), Stream::toList)),
          collector("parallelToOrderedStream(e) via config", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, Config.with().executor(e()).build()), Stream::toList)),
          collector("parallelToOrderedStream(e, p) via config", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, Config.with().executor(e()).parallelism(p()).build()), Stream::toList)),
          collector("parallelToOrderedStream(e, p) [batching] via config", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, Config.with().batching(true).executor(e()).parallelism(p()).build()), Stream::toList)),
          //
          collector("parallel()", f -> collectingAndThen(ParallelCollectors.parallel(f), c -> c.join().toList())),
          collector("parallel(p)", f -> collectingAndThen(ParallelCollectors.parallel(f, p()), c -> c.join().toList())),
          collector("parallel(e)", f -> collectingAndThen(ParallelCollectors.parallel(f, e()), c -> c.join().toList())),
          collector("parallel(e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, e(), p()), c -> c.join().toList())),
          collector("parallel(toList())", f -> collectingAndThen(ParallelCollectors.parallel(f, toList()), CompletableFuture::join)),
          collector("parallel(toList(), p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e()), CompletableFuture::join)),
          collector("parallel(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallelToOrderedStream()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f), Stream::toList)),
          collector("parallelToOrderedStream(p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, p()), Stream::toList)),
          collector("parallelToOrderedStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e()), Stream::toList)),
          collector("parallelToOrderedStream(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStream(e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p()), Stream::toList))
        );
    }

    static Stream<Factory.GenericCollector<Factory.CollectorFactoryWithParallelism<Integer, Integer>>> allBounded() {
        return Stream.of(
          limitedCollector("parallel(p) via config", (f,p) -> collectingAndThen(ParallelCollectors.parallel(f, Config.with().parallelism(p).build()), c -> c.join().toList())),
          limitedCollector("parallel(e, p) via config", (f,p) -> collectingAndThen(ParallelCollectors.parallel(f, Config.with().executor(e()).parallelism(p).build()), c -> c.join().toList())),
          limitedCollector("parallel(toList(), e, p) [batching] via config", (f,p) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), Config.with().batching(true).executor(e()).parallelism(p).build()), CompletableFuture::join)),
          limitedCollector("parallel(toList(), p) via config", (f,p) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), Config.with().parallelism(p).build()), CompletableFuture::join)),
          limitedCollector("parallel(toList(), e, p) via config", (f,p) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), Config.with().executor(e()).parallelism(p).build()), CompletableFuture::join)),
          limitedCollector("parallelToStream(p) via config", (f,p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, Config.with().parallelism(p).build()), Stream::toList)),
          limitedCollector("parallelToStream(e, p) via config", (f,p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, Config.with().executor(e()).parallelism(p).build()), Stream::toList)),
          limitedCollector("parallelToStream(e, p) [batching] via config", (f,p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, Config.with().batching(true).executor(e()).parallelism(p).build()), Stream::toList)),
          limitedCollector("parallelToOrderedStream(p) via config", (f,p) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, Config.with().parallelism(p).build()), Stream::toList)),
          limitedCollector("parallelToOrderedStream(e, p) via config", (f,p) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, Config.with().executor(e()).parallelism(p).build()), Stream::toList)),
          limitedCollector("parallelToOrderedStream(e, p) [batching] via config", (f,p) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, Config.with().batching(true).executor(e()).parallelism(p).build()), Stream::toList)),
          //
          limitedCollector("parallel(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, p), c -> c.join().toList())),
          limitedCollector("parallel(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, e(), p), c -> c.join().toList())),
          limitedCollector("parallel(toList(), p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), p), CompletableFuture::join)),
          limitedCollector("parallel(toList(), e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e(), p), CompletableFuture::join)),
          limitedCollector("parallel(toList(), e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e(), p), CompletableFuture::join)),
          limitedCollector("parallelToStream(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, p), Stream::toList)),
          limitedCollector("parallelToStream(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, e(), p), Stream::toList)),
          limitedCollector("parallelToStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e(), p), Stream::toList)),
          limitedCollector("parallelToOrderedStream(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, p), Stream::toList)),
          limitedCollector("parallelToOrderedStream(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e(), p), Stream::toList)),
          limitedCollector("parallelToOrderedStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p), Stream::toList))
        );
    }

    public static Stream<GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>>> boundedCollectors() {
        return Stream.of(
          advancedCollector("parallel() via config", (f, e, p) -> ParallelCollectors.parallel(f, Config.with().executor(e).parallelism(p).build())),
          advancedCollector("parallel(toList()) via config", (f, e, p) -> ParallelCollectors.parallel(f, toList(), Config.with().executor(e).parallelism(p).build())),
          advancedCollector("parallelToStream() via config", (f, e, p) -> ParallelCollectors.parallelToStream(f, Config.with().executor(e).parallelism(p).build())),
          advancedCollector("parallelToOrderedStream() via config", (f, e, p) -> ParallelCollectors.parallelToOrderedStream(f, Config.with().executor(e).parallelism(p).build())),
          advancedCollector("parallel() (batching) via config", (f, e, p) -> ParallelCollectors.parallel(f, Config.with().executor(e).batching(true).parallelism(p).build())),
          advancedCollector("parallel(toList()) (batching) via config", (f, e, p) -> ParallelCollectors.parallel(f, toList(), Config.with().executor(e).batching(true).parallelism(p).build())),
          advancedCollector("parallelToStream() (batching) via config", (f, e, p) -> ParallelCollectors.parallelToStream(f, Config.with().executor(e).batching(true).parallelism(p).build())),
          advancedCollector("parallelToOrderedStream() (batching) via config", (f, e, p) -> ParallelCollectors.parallelToOrderedStream(f, Config.with().executor(e).batching(true).parallelism(p).build())),
          //
          advancedCollector("parallel()", (f, e, p) -> ParallelCollectors.parallel(f, e, p)),
          advancedCollector("parallel(toList())", (f, e, p) -> ParallelCollectors.parallel(f, toList(), e, p)),
          advancedCollector("parallelToStream()", (f, e, p) -> ParallelCollectors.parallelToStream(f, e, p)),
          advancedCollector("parallelToOrderedStream()", (f, e, p) -> ParallelCollectors.parallelToOrderedStream(f, e, p)),
          advancedCollector("parallel() (batching)", (f, e, p) -> ParallelCollectors.Batching.parallel(f, e, p)),
          advancedCollector("parallel(toList()) (batching)", (f, e, p) -> ParallelCollectors.Batching.parallel(f, toList(), e, p)),
          advancedCollector("parallelToStream() (batching)", (f, e, p) -> ParallelCollectors.Batching.parallelToStream(f, e, p)),
          advancedCollector("parallelToOrderedStream() (batching)", (f, e, p) -> ParallelCollectors.Batching.parallelToOrderedStream(f, e, p)));
    }

    @FunctionalInterface
    interface CollectorFactoryWithParallelismAndExecutor<T, R> {
        Collector<T, ?, ?> apply(Function<T, R> function, Executor executorService, int parallelism);
    }

    @FunctionalInterface
    interface CollectorFactoryWithExecutor<T, R> {
        Collector<T, ?, List<R>> collector(Function<T, R> f, Executor executor);
    }

    @FunctionalInterface
    interface CollectorFactoryWithParallelism<T, R> {
        Collector<T, ?, List<R>> collector(Function<T, R> f, Integer p);
    }

    @FunctionalInterface
    interface CollectorFactory<T, R> {
        Collector<T, ?, List<R>> collector(Function<T, R> f);
    }

    @FunctionalInterface
    interface StreamingCollectorFactory<T, R> {
        Collector<T, ?, Stream<R>> collector(Function<T, R> f);
    }

    @FunctionalInterface
    interface AsyncCollectorFactory<T, R> {
        Collector<T, ?, CompletableFuture<List<R>>> collector(Function<T, R> f);
    }

    record GenericCollector<T>(String name, T factory) {
        static <T, R> GenericCollector<Factory.CollectorFactory<T, R>> collector(String name, Factory.CollectorFactory<T, R> collector) {
            return new GenericCollector<>(name, collector);
        }

        static <T, R> GenericCollector<Factory.AsyncCollectorFactory<T, R>> asyncCollector(String name, Factory.AsyncCollectorFactory<T, R> collector) {
            return new GenericCollector<>(name, collector);
        }

        static <T, R> GenericCollector<Factory.StreamingCollectorFactory<T, R>> streamingCollector(String name, Factory.StreamingCollectorFactory<T, R> collector) {
            return new GenericCollector<>(name, collector);
        }

        static <T, R> GenericCollector<CollectorFactoryWithParallelism<T, R>> limitedCollector(String name, CollectorFactoryWithParallelism<T, R> collector) {
            return new GenericCollector<>(name, collector);
        }

        static <T, R> GenericCollector<CollectorFactoryWithExecutor<T, R>> executorCollector(String name, CollectorFactoryWithExecutor<T, R> collector) {
            return new GenericCollector<>(name, collector);
        }

        static <T, R> GenericCollector<CollectorFactoryWithParallelismAndExecutor<T, R>> advancedCollector(String name, CollectorFactoryWithParallelismAndExecutor<T, R> collector) {
            return new GenericCollector<>(name, collector);
        }
    }

    static Executor e() {
        return Executors.newCachedThreadPool();
    }

    static int p() {
        return 4;
    }
}
