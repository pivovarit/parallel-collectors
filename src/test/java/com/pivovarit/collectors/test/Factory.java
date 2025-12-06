package com.pivovarit.collectors.test;

import com.pivovarit.collectors.ParallelCollectors;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.Batching.parallel;
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
          collector("parallel()", f -> collectingAndThen(ParallelCollectors.parallel(f), c -> c.join().toList())),
          collector("parallel(p)", f -> collectingAndThen(ParallelCollectors.parallel(f, p()), c -> c.join().toList())),
          collector("parallel(e)", f -> collectingAndThen(ParallelCollectors.parallel(f, e()), c -> c.join().toList())),
          collector("parallel(e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, e(), p()), c -> c.join().toList())),
          collector("parallel(toList())", f -> collectingAndThen(ParallelCollectors.parallel(f, toList()), CompletableFuture::join)),
          collector("parallel(toList(), p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e()), CompletableFuture::join)),
          collector("parallel(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallelBy()", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f), c -> c.join().toList())),
          collector("parallelBy(p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, p()), c -> c.join().toList())),
          collector("parallelBy(e)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, e()), c -> c.join().toList())),
          collector("parallelBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, e(), p()), c -> c.join().toList())),
          collector("parallelBy(toList())", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList()), CompletableFuture::join)),
          collector("parallelBy(toList(), p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), p()), CompletableFuture::join)),
          collector("parallelBy(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), e()), CompletableFuture::join)),
          collector("parallelBy(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallelToStream()", f -> collectingAndThen(ParallelCollectors.parallelToStream(f), Stream::toList)),
          collector("parallelToStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, p()), Stream::toList)),
          collector("parallelToStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, e()), Stream::toList)),
          collector("parallelToStream(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStream(f, e(), p()), Stream::toList)),
          collector("parallelToStream(e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e(), p()), Stream::toList)),
          collector("parallelToStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f), Stream::toList)),
          collector("parallelToStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, p()), Stream::toList)),
          collector("parallelToStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, e()), Stream::toList)),
          collector("parallelToStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStream()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f), Stream::toList)),
          collector("parallelToOrderedStream(p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, p()), Stream::toList)),
          collector("parallelToOrderedStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e()), Stream::toList)),
          collector("parallelToOrderedStream(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStream(e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f), Stream::toList)),
          collector("parallelToOrderedStreamBy(p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, p()), Stream::toList)),
          collector("parallelToOrderedStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, e()), Stream::toList)),
          collector("parallelToOrderedStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, e(), p()), Stream::toList))
        );
    }

    static <T> Stream<Factory.GenericCollector<Factory.CollectorFactory<Integer, Integer>>> allGrouping(Function<Integer, T> classifier, int parallelism) {
        return Stream.of(
          collector("parallelBy()", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f), c -> c.join().toList())),
          collector("parallelBy(p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, parallelism), c -> c.join().toList())),
          collector("parallelBy(e)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, e()), c -> c.join().toList())),
          collector("parallelBy(p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, parallelism), c -> c.join().toList())),
          collector("parallelBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, e(), parallelism), c -> c.join().toList())),
          collector("parallelBy(toList())", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, toList()), CompletableFuture::join)),
          collector("parallelBy(toList(), p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, toList(), parallelism), CompletableFuture::join)),
          collector("parallelBy(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, toList(), e()), CompletableFuture::join)),
          collector("parallelBy(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(classifier, f, toList(), e(), parallelism), CompletableFuture::join)),
          collector("parallelToStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f), Stream::toList)),
          collector("parallelToStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, parallelism), Stream::toList)),
          collector("parallelToStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, e()), Stream::toList)),
          collector("parallelToStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToStreamBy(classifier, f, e(), parallelism), Stream::toList)),
          collector("parallelToOrderedStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(classifier, f), Stream::toList)),
          collector("parallelToOrderedStreamBy(p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(classifier, f, parallelism), Stream::toList)),
          collector("parallelToOrderedStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(classifier, f, e()), Stream::toList)),
          collector("parallelToOrderedStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(classifier, f, e(), parallelism), Stream::toList))
        );
    }

    static Stream<Factory.GenericCollector<Factory.CollectorFactory<Integer, Integer>>> allOrdered() {
        return Stream.of(
          collector("parallel()", f -> collectingAndThen(ParallelCollectors.parallel(f), c -> c.join().toList())),
          collector("parallel(p)", f -> collectingAndThen(ParallelCollectors.parallel(f, p()), c -> c.join().toList())),
          collector("parallel(e)", f -> collectingAndThen(ParallelCollectors.parallel(f, e()), c -> c.join().toList())),
          collector("parallel(e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, e(), p()), c -> c.join().toList())),
          collector("parallelBy()", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f), c -> c.join().toList())),
          collector("parallelBy(p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, p()), c -> c.join().toList())),
          collector("parallelBy(e)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, e()), c -> c.join().toList())),
          collector("parallelBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, e(), p()), c -> c.join().toList())),
          collector("parallel(toList())", f -> collectingAndThen(ParallelCollectors.parallel(f, toList()), CompletableFuture::join)),
          collector("parallel(toList(), p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e()), CompletableFuture::join)),
          collector("parallel(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallel(toList(), e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallelBy(toList())", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList()), CompletableFuture::join)),
          collector("parallelBy(toList(), p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), p()), CompletableFuture::join)),
          collector("parallelBy(toList(), e)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), e()), CompletableFuture::join)),
          collector("parallelBy(toList(), e, p)", f -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), e(), p()), CompletableFuture::join)),
          collector("parallelToOrderedStream()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f), Stream::toList)),
          collector("parallelToOrderedStream(p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, p()), Stream::toList)),
          collector("parallelToOrderedStream(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e()), Stream::toList)),
          collector("parallelToOrderedStream(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStream(e, p) [batching]", f -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p()), Stream::toList)),
          collector("parallelToOrderedStreamBy()", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f), Stream::toList)),
          collector("parallelToOrderedStreamBy(p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, p()), Stream::toList)),
          collector("parallelToOrderedStreamBy(e)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, e()), Stream::toList)),
          collector("parallelToOrderedStreamBy(e, p)", f -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, e(), p()), Stream::toList))
        );
    }

    static Stream<Factory.GenericCollector<Factory.CollectorFactoryWithParallelism<Integer, Integer>>> allBounded() {
        return Stream.of(
          limitedCollector("parallel(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, p), c -> c.join().toList())),
          limitedCollector("parallel(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, e(), p), c -> c.join().toList())),
          limitedCollector("parallel(toList(), p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), p), CompletableFuture::join)),
          limitedCollector("parallel(toList(), e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), e(), p), CompletableFuture::join)),
          limitedCollector("parallel(toList(), e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallel(f, toList(), e(), p), CompletableFuture::join)),
          limitedCollector("parallelBy(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, p), c -> c.join().toList())),
          limitedCollector("parallelBy(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, e(), p), c -> c.join().toList())),
          limitedCollector("parallelBy(toList(), p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), p), CompletableFuture::join)),
          limitedCollector("parallelBy(toList(), e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelBy(noopClassifier(), f, toList(), e(), p), CompletableFuture::join)),
          limitedCollector("parallelToStream(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, p), Stream::toList)),
          limitedCollector("parallelToStream(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStream(f, e(), p), Stream::toList)),
          limitedCollector("parallelToStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallelToStream(f, e(), p), Stream::toList)),
          limitedCollector("parallelToStreamBy(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, p), Stream::toList)),
          limitedCollector("parallelToStreamBy(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToStreamBy(noopClassifier(), f, e(), p), Stream::toList)),
          limitedCollector("parallelToOrderedStream(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, p), Stream::toList)),
          limitedCollector("parallelToOrderedStream(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToOrderedStream(f, e(), p), Stream::toList)),
          limitedCollector("parallelToOrderedStream(e, p) [batching]", (f, p) -> collectingAndThen(ParallelCollectors.Batching.parallelToOrderedStream(f, e(), p), Stream::toList)),
          limitedCollector("parallelToOrderedStreamBy(p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, p), Stream::toList)),
          limitedCollector("parallelToOrderedStreamBy(e, p)", (f, p) -> collectingAndThen(ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, e(), p), Stream::toList))
        );
    }

    public static Stream<GenericCollector<CollectorFactoryWithParallelismAndExecutor<Integer, Integer>>> boundedCollectors() {
        return Stream.of(
          advancedCollector("parallel()", (f, e, p) -> ParallelCollectors.parallel(f, e, p)),
          advancedCollector("parallel(toList())", (f, e, p) -> ParallelCollectors.parallel(f, toList(), e, p)),
          advancedCollector("parallelBy()", (f, e, p) -> ParallelCollectors.parallelBy(noopClassifier(), f, e, p)),
          advancedCollector("parallelBy(toList())", (f, e, p) -> ParallelCollectors.parallelBy(noopClassifier(), f, toList(), e, p)),
          advancedCollector("parallelToStream()", (f, e, p) -> ParallelCollectors.parallelToStream(f, e, p)),
          advancedCollector("parallelToStreamBy()", (f, e, p) -> ParallelCollectors.parallelToStreamBy(noopClassifier(), f, e, p)),
          advancedCollector("parallelToOrderedStream()", (f, e, p) -> ParallelCollectors.parallelToOrderedStream(f, e, p)),
          advancedCollector("parallelToOrderedStreamBy()", (f, e, p) -> ParallelCollectors.parallelToOrderedStreamBy(noopClassifier(), f, e, p)),
          advancedCollector("parallel() (batching)", (f, e, p) -> parallel(f, e, p)),
          advancedCollector("parallel(toList()) (batching)", (f, e, p) -> parallel(f, toList(), e, p)),
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

    static <T> Function<T, UUID> noopClassifier() {
        return i -> UUID.randomUUID();
    }
}
