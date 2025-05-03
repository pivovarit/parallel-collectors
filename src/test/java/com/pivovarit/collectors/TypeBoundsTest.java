package com.pivovarit.collectors;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;

class TypeBoundsTest {

    private final Function<Integer, String> its = i -> i.toString();
    private final Function<Object, String> ots = i -> i.toString();

    @Test
    void shouldCompileBatchingParallel() {
        Collector<Integer, ?, CompletableFuture<Stream<String>>> collector;
        collector = ParallelCollectors.Batching.parallel(its, r -> {}, 42);
        collector = ParallelCollectors.Batching.parallel(ots, r -> {}, 42);
    }

    @Test
    void shouldCompileBatchingParallelToList() {
        Collector<Integer, ?, CompletableFuture<List<String>>> collector;
        collector = ParallelCollectors.Batching.parallel(its, toList(), r -> {}, 42);
        collector = ParallelCollectors.Batching.parallel(ots, toList(), r -> {}, 42);
    }

    @Test
    void shouldCompileBatchingStreaming() {
        Collector<Integer, ?, Stream<String>> collector;
        collector = ParallelCollectors.Batching.parallelToStream(its, r -> {}, 42);
        collector = ParallelCollectors.Batching.parallelToStream(ots, r -> {}, 42);

        collector = ParallelCollectors.Batching.parallelToOrderedStream(its, r -> {}, 42);
        collector = ParallelCollectors.Batching.parallelToOrderedStream(ots, r -> {}, 42);
    }

    @Test
    void shouldCompileParallel() {
        Collector<Integer, ?, CompletableFuture<Stream<String>>> collector;
        collector = ParallelCollectors.parallel(its, r -> {}, 42);
        collector = ParallelCollectors.parallel(ots, r -> {}, 42);

        collector = ParallelCollectors.parallel(its, r -> {});
        collector = ParallelCollectors.parallel(ots, r -> {});

        collector = ParallelCollectors.parallel(its);
        collector = ParallelCollectors.parallel(ots);
    }

    @Test
    void shouldCompileParallelToList() {
        Collector<Integer, ?, CompletableFuture<List<String>>> collector;
        collector = ParallelCollectors.parallel(its, toList(), r -> {}, 42);
        collector = ParallelCollectors.parallel(ots, toList(), r -> {}, 42);

        collector = ParallelCollectors.parallel(its, toList(), r -> {});
        collector = ParallelCollectors.parallel(ots, toList(), r -> {});

        collector = ParallelCollectors.parallel(its, toList());
        collector = ParallelCollectors.parallel(ots, toList());
    }

    @Test
    void shouldCompileStreaming() {
        Collector<Integer, ?, Stream<String>> collector;
        collector = ParallelCollectors.parallelToStream(its, r -> {}, 42);
        collector = ParallelCollectors.parallelToStream(ots, r -> {}, 42);

        collector = ParallelCollectors.parallelToStream(its, r -> {});
        collector = ParallelCollectors.parallelToStream(ots, r -> {});

        collector = ParallelCollectors.parallelToOrderedStream(its, r -> {}, 42);
        collector = ParallelCollectors.parallelToOrderedStream(ots, r -> {}, 42);

        collector = ParallelCollectors.parallelToOrderedStream(its, r -> {});
        collector = ParallelCollectors.parallelToOrderedStream(ots, r -> {});
    }
}
