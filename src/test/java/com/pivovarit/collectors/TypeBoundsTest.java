package com.pivovarit.collectors;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;

class TypeBoundsTest {

    @Nested
    class Covariance {

        private final Function<Integer, Number> itn = i -> i;
        private final Function<Integer, Integer> iti = i -> i;

        @Test
        void shouldCompileBatchingParallel() {
            Collector<Integer, ?, CompletableFuture<Stream<Number>>> collector;
            collector = ParallelCollectors.Batching.parallel(itn, r -> {}, 42);
            collector = ParallelCollectors.Batching.parallel(iti, r -> {}, 42);
        }

        @Test
        void shouldCompileBatchingParallelToList() {
            Collector<Integer, ?, CompletableFuture<List<Number>>> collector;
            collector = ParallelCollectors.Batching.parallel(itn, toList(), r -> {}, 42);
            collector = ParallelCollectors.Batching.parallel(iti, toList(), r -> {}, 42);
        }

        @Test
        void shouldCompileBatchingStreaming() {
            Collector<Integer, ?, Stream<Number>> collector;
            collector = ParallelCollectors.Batching.parallelToStream(itn, r -> {}, 42);
            collector = ParallelCollectors.Batching.parallelToStream(iti, r -> {}, 42);

            collector = ParallelCollectors.Batching.parallelToOrderedStream(itn, r -> {}, 42);
            collector = ParallelCollectors.Batching.parallelToOrderedStream(iti, r -> {}, 42);
        }

        @Test
        void shouldCompileParallel() {
            Collector<Integer, ?, CompletableFuture<Stream<Number>>> collector;
            collector = ParallelCollectors.parallel(itn, r -> {}, 42);
            collector = ParallelCollectors.parallel(iti, r -> {}, 42);

            collector = ParallelCollectors.parallel(itn, r -> {});
            collector = ParallelCollectors.parallel(iti, r -> {});

            collector = ParallelCollectors.parallel(itn, 42);
            collector = ParallelCollectors.parallel(iti, 42);

            collector = ParallelCollectors.parallel(itn);
            collector = ParallelCollectors.parallel(iti);
        }

        @Test
        void shouldCompileParallelToList() {
            Collector<Integer, ?, CompletableFuture<List<Number>>> collector;
            collector = ParallelCollectors.parallel(itn, toList(), r -> {}, 42);
            collector = ParallelCollectors.parallel(iti, toList(), r -> {}, 42);

            collector = ParallelCollectors.parallel(itn, toList(), r -> {});
            collector = ParallelCollectors.parallel(iti, toList(), r -> {});

            collector = ParallelCollectors.parallel(itn, toList(), 42);
            collector = ParallelCollectors.parallel(iti, toList(), 42);

            collector = ParallelCollectors.parallel(itn, toList());
            collector = ParallelCollectors.parallel(iti, toList());
        }

        @Test
        void shouldCompileStreaming() {
            Collector<Integer, ?, Stream<Number>> collector;
            collector = ParallelCollectors.parallelToStream(itn, r -> {}, 42);
            collector = ParallelCollectors.parallelToStream(iti, r -> {}, 42);

            collector = ParallelCollectors.parallelToStream(itn, r -> {});
            collector = ParallelCollectors.parallelToStream(iti, r -> {});

            collector = ParallelCollectors.parallelToOrderedStream(itn, r -> {}, 42);
            collector = ParallelCollectors.parallelToOrderedStream(iti, r -> {}, 42);

            collector = ParallelCollectors.parallelToOrderedStream(itn, r -> {});
            collector = ParallelCollectors.parallelToOrderedStream(iti, r -> {});
        }
    }

    @Nested
    class Contravariance {

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

            collector = ParallelCollectors.parallel(its, 42);
            collector = ParallelCollectors.parallel(ots, 42);

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

            collector = ParallelCollectors.parallel(its, toList(), 42);
            collector = ParallelCollectors.parallel(ots, toList(), 42);

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
}
