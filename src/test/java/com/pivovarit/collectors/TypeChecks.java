package com.pivovarit.collectors;

import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.stream.Collectors.toList;

/**
 * This file exists solely to check that the public API
 * exposes correct generic bounds (covariance / contravariance).
 * It must compile. It is not intended to run.
 */
@SuppressWarnings({"unused"})
final class TypeChecks {

    private TypeChecks() {
    }

    private static <A, R> void expectCollector(Collector<A, ?, R> c) {
        // compile-time only
    }

    /* ============================================================
     * COVARIANCE
     * Integer -> Integer  (identity)
     * Integer -> Number   (wider)
     * ============================================================ */
    private static final class Covariance {
        private static final Function<Integer, Number> itn = i -> i;
        private static final Function<Integer, Integer> iti = i -> i;

        static {
            // ---------- Batching.parallel ----------
            expectCollector(ParallelCollectors.Batching.parallel(itn, r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallel(iti, r -> {}, 42));

            // ---------- Batching.parallel (toList) ----------
            expectCollector(ParallelCollectors.Batching.parallel(itn, toList(), r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallel(iti, toList(), r -> {}, 42));

            // ---------- Batching.parallelToStream ----------
            expectCollector(ParallelCollectors.Batching.parallelToStream(itn, r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallelToStream(iti, r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallelToOrderedStream(itn, r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallelToOrderedStream(iti, r -> {}, 42));

            // ---------- parallel ----------
            expectCollector(ParallelCollectors.parallel(itn, r -> {}, 42));
            expectCollector(ParallelCollectors.parallel(iti, r -> {}, 42));

            expectCollector(ParallelCollectors.parallel(itn, r -> {}));
            expectCollector(ParallelCollectors.parallel(iti, r -> {}));

            expectCollector(ParallelCollectors.parallel(itn, 42));
            expectCollector(ParallelCollectors.parallel(iti, 42));

            expectCollector(ParallelCollectors.parallel(itn));
            expectCollector(ParallelCollectors.parallel(iti));

            // ---------- parallel (toList) ----------
            expectCollector(ParallelCollectors.parallel(itn, toList(), r -> {}, 42));
            expectCollector(ParallelCollectors.parallel(iti, toList(), r -> {}, 42));

            expectCollector(ParallelCollectors.parallel(itn, toList(), r -> {}));
            expectCollector(ParallelCollectors.parallel(iti, toList(), r -> {}));

            expectCollector(ParallelCollectors.parallel(itn, toList(), 42));
            expectCollector(ParallelCollectors.parallel(iti, toList(), 42));

            expectCollector(ParallelCollectors.parallel(itn, toList()));
            expectCollector(ParallelCollectors.parallel(iti, toList()));

            // ---------- streaming ----------
            expectCollector(ParallelCollectors.parallelToStream(itn, r -> {}, 42));
            expectCollector(ParallelCollectors.parallelToStream(iti, r -> {}, 42));

            expectCollector(ParallelCollectors.parallelToStream(itn, r -> {}));
            expectCollector(ParallelCollectors.parallelToStream(iti, r -> {}));

            expectCollector(ParallelCollectors.parallelToOrderedStream(itn, r -> {}, 42));
            expectCollector(ParallelCollectors.parallelToOrderedStream(iti, r -> {}, 42));

            expectCollector(ParallelCollectors.parallelToOrderedStream(itn, r -> {}));
            expectCollector(ParallelCollectors.parallelToOrderedStream(iti, r -> {}));
        }
    }

    /* ============================================================
     * CONTRAVARIANCE
     * Integer -> String   (narrow)
     * Object  -> String   (wider argument type)
     * ============================================================ */
    private static final class Contravariance {
        private static final Function<Integer, String> its = Object::toString;
        private static final Function<Object, String> ots = Object::toString;

        static {
            // ---------- Batching.parallel ----------
            expectCollector(ParallelCollectors.Batching.parallel(its, r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallel(ots, r -> {}, 42));

            // ---------- Batching.parallel (toList) ----------
            expectCollector(ParallelCollectors.Batching.parallel(its, toList(), r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallel(ots, toList(), r -> {}, 42));

            // ---------- Batching.parallelToStream ----------
            expectCollector(ParallelCollectors.Batching.parallelToStream(its, r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallelToStream(ots, r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallelToOrderedStream(its, r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallelToOrderedStream(ots, r -> {}, 42));

            // ---------- parallel ----------
            expectCollector(ParallelCollectors.parallel(its, r -> {}, 42));
            expectCollector(ParallelCollectors.parallel(ots, r -> {}, 42));

            expectCollector(ParallelCollectors.parallel(its, r -> {}));
            expectCollector(ParallelCollectors.parallel(ots, r -> {}));

            expectCollector(ParallelCollectors.parallel(its, 42));
            expectCollector(ParallelCollectors.parallel(ots, 42));

            expectCollector(ParallelCollectors.parallel(its));
            expectCollector(ParallelCollectors.parallel(ots));

            // ---------- parallel (toList) ----------
            expectCollector(ParallelCollectors.parallel(its, toList(), r -> {}, 42));
            expectCollector(ParallelCollectors.parallel(ots, toList(), r -> {}, 42));

            expectCollector(ParallelCollectors.parallel(its, toList(), r -> {}));
            expectCollector(ParallelCollectors.parallel(ots, toList(), r -> {}));

            expectCollector(ParallelCollectors.parallel(its, toList(), 42));
            expectCollector(ParallelCollectors.parallel(ots, toList(), 42));

            expectCollector(ParallelCollectors.parallel(its, toList()));
            expectCollector(ParallelCollectors.parallel(ots, toList()));

            // ---------- streaming ----------
            expectCollector(ParallelCollectors.parallelToStream(its, r -> {}, 42));
            expectCollector(ParallelCollectors.parallelToStream(ots, r -> {}, 42));

            expectCollector(ParallelCollectors.parallelToStream(its, r -> {}));
            expectCollector(ParallelCollectors.parallelToStream(ots, r -> {}));

            expectCollector(ParallelCollectors.parallelToOrderedStream(its, r -> {}, 42));
            expectCollector(ParallelCollectors.parallelToOrderedStream(ots, r -> {}, 42));

            expectCollector(ParallelCollectors.parallelToOrderedStream(its, r -> {}));
            expectCollector(ParallelCollectors.parallelToOrderedStream(ots, r -> {}));
        }
    }
}
