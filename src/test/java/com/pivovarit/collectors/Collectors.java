package com.pivovarit.collectors;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.Batching.parallel;
import static java.util.stream.Collectors.toList;

public final class Collectors {

    private Collectors() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Stream<Map.Entry<String, BoundedCollectorFactory<Integer>>> boundedCollectors() {
        return Stream.of(
          Map.entry("parallel()", (f, e, p) -> ParallelCollectors.parallel(f, e, p)),
          Map.entry("parallel(toList())", (f, e, p) -> ParallelCollectors.parallel(f, toList(), e, p)),
          Map.entry("parallelToStream()", (f, e, p) -> ParallelCollectors.parallelToStream(f, e, p)),
          Map.entry("parallelToOrderedStream()", (f, e, p) -> ParallelCollectors.parallelToOrderedStream(f, e, p)),
          Map.entry("parallel() (batching)", (f, e, p) -> parallel(f, e, p)),
          Map.entry("parallel(toList()) (batching)", (f, e, p) -> parallel(f, toList(), e, p)),
          Map.entry("parallelToStream() (batching)", (f, e, p) -> ParallelCollectors.Batching.parallelToStream(f, e, p)),
          Map.entry("parallelToOrderedStream() (batching)", (f, e, p) -> ParallelCollectors.Batching.parallelToOrderedStream(f, e, p)));
    }

    public interface BoundedCollectorFactory<T> {
        Collector<T, ?, ?> apply(Function<T, ?> function, Executor executorService, int parallelism);
    }
}
