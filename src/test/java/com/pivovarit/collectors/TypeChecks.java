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

    static class SuperClass {
    }

    static class SubClass extends SuperClass {
    }

    private static <A, R> void expectCollector(Collector<A, ?, R> c) {
        // compile-only
    }

    /* ============================================================
     * COVARIANCE
     *
     * Function<SubClass, SuperClass>   — wider return type
     * Function<SubClass, SubClass>     — exact return type
     * ============================================================ */
    private static final class Covariance {
        private static final Function<SubClass, SuperClass> subToSuper = x -> x;
        private static final Function<SubClass, SubClass> subToSub = x -> x;

        static {
            // ---------- Batching.parallel ----------
            expectCollector(ParallelCollectors.Batching.parallel(subToSuper, r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallel(subToSub, r -> {}, 42));

            // ---------- Batching.parallel (toList) ----------
            expectCollector(ParallelCollectors.Batching.parallel(subToSuper, toList(), r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallel(subToSub, toList(), r -> {}, 42));

            // ---------- Batching.parallelToStream ----------
            expectCollector(ParallelCollectors.Batching.parallelToStream(subToSuper, r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallelToStream(subToSub, r -> {}, 42));

            expectCollector(ParallelCollectors.Batching.parallelToOrderedStream(subToSuper, r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallelToOrderedStream(subToSub, r -> {}, 42));

            // ---------- parallel ----------
            expectCollector(ParallelCollectors.parallel(subToSuper, r -> {}, 42));
            expectCollector(ParallelCollectors.parallel(subToSub, r -> {}, 42));

            expectCollector(ParallelCollectors.parallel(subToSuper, r -> {}));
            expectCollector(ParallelCollectors.parallel(subToSub, r -> {}));

            expectCollector(ParallelCollectors.parallel(subToSuper, 42));
            expectCollector(ParallelCollectors.parallel(subToSub, 42));

            expectCollector(ParallelCollectors.parallel(subToSuper));
            expectCollector(ParallelCollectors.parallel(subToSub));

            // ---------- parallelBy ----------
            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSuper, r -> {}, 42));
            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSub, r -> {}, 42));

            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSuper, r -> {}));
            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSub, r -> {}));

            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSuper, 42));
            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSub, 42));

            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSuper));
            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSub));

            // ---------- parallel (toList) ----------
            expectCollector(ParallelCollectors.parallel(subToSuper, toList(), r -> {}, 42));
            expectCollector(ParallelCollectors.parallel(subToSub, toList(), r -> {}, 42));

            expectCollector(ParallelCollectors.parallel(subToSuper, toList(), r -> {}));
            expectCollector(ParallelCollectors.parallel(subToSub, toList(), r -> {}));

            expectCollector(ParallelCollectors.parallel(subToSuper, toList(), 42));
            expectCollector(ParallelCollectors.parallel(subToSub, toList(), 42));

            expectCollector(ParallelCollectors.parallel(subToSuper, toList()));
            expectCollector(ParallelCollectors.parallel(subToSub, toList()));

            // ---------- parallelBy (toList) ----------
            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSuper, toList(), r -> {}, 42));
            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSub, toList(), r -> {}, 42));

            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSuper, toList(), r -> {}));
            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSub, toList(), r -> {}));

            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSuper, toList(), 42));
            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSub, toList(), 42));

            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSuper, toList()));
            expectCollector(ParallelCollectors.parallelBy(x -> x, subToSub, toList()));

            // ---------- streaming ----------
            expectCollector(ParallelCollectors.parallelToStream(subToSuper, r -> {}, 42));
            expectCollector(ParallelCollectors.parallelToStream(subToSub, r -> {}, 42));

            expectCollector(ParallelCollectors.parallelToStream(subToSuper, r -> {}));
            expectCollector(ParallelCollectors.parallelToStream(subToSub, r -> {}));

            expectCollector(ParallelCollectors.parallelToOrderedStream(subToSuper, r -> {}, 42));
            expectCollector(ParallelCollectors.parallelToOrderedStream(subToSub, r -> {}, 42));

            expectCollector(ParallelCollectors.parallelToOrderedStream(subToSuper, r -> {}));
            expectCollector(ParallelCollectors.parallelToOrderedStream(subToSub, r -> {}));

            // ---------- streamingBy ----------
            expectCollector(ParallelCollectors.parallelToStreamBy(x -> x, subToSuper, r -> {}, 42));
            expectCollector(ParallelCollectors.parallelToStreamBy(x -> x, subToSub, r -> {}, 42));

            expectCollector(ParallelCollectors.parallelToStreamBy(x -> x, subToSuper, r -> {}));
            expectCollector(ParallelCollectors.parallelToStreamBy(x -> x, subToSub, r -> {}));

            expectCollector(ParallelCollectors.parallelToOrderedStreamBy(x -> x, subToSuper, r -> {}, 42));
            expectCollector(ParallelCollectors.parallelToOrderedStreamBy(x -> x, subToSub, r -> {}, 42));

            expectCollector(ParallelCollectors.parallelToOrderedStreamBy(x -> x, subToSuper, r -> {}));
            expectCollector(ParallelCollectors.parallelToOrderedStreamBy(x -> x, subToSub, r -> {}));
        }
    }

    /* ============================================================
     * CONTRAVARIANCE
     *
     * Function<SuperClass, SubClass>   — argument is wider
     * Function<Object, SubClass>       — argument even wider
     *
     * (Return type same; argument varies)
     * ============================================================ */
    private static final class Contravariance {
        private static final Function<SuperClass, SubClass> superToSub = x -> new SubClass();
        private static final Function<Object, SubClass> objToSub = x -> new SubClass();

        static {
            // ---------- Batching.parallel ----------
            expectCollector(ParallelCollectors.Batching.parallel(superToSub, r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallel(objToSub, r -> {}, 42));

            // ---------- Batching.parallel (toList) ----------
            expectCollector(ParallelCollectors.Batching.parallel(superToSub, toList(), r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallel(objToSub, toList(), r -> {}, 42));

            // ---------- Batching.parallelToStream ----------
            expectCollector(ParallelCollectors.Batching.parallelToStream(superToSub, r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallelToStream(objToSub, r -> {}, 42));

            expectCollector(ParallelCollectors.Batching.parallelToOrderedStream(superToSub, r -> {}, 42));
            expectCollector(ParallelCollectors.Batching.parallelToOrderedStream(objToSub, r -> {}, 42));

            // ---------- parallel ----------
            expectCollector(ParallelCollectors.parallel(superToSub, r -> {}, 42));
            expectCollector(ParallelCollectors.parallel(objToSub, r -> {}, 42));

            expectCollector(ParallelCollectors.parallel(superToSub, r -> {}));
            expectCollector(ParallelCollectors.parallel(objToSub, r -> {}));

            expectCollector(ParallelCollectors.parallel(superToSub, 42));
            expectCollector(ParallelCollectors.parallel(objToSub, 42));

            expectCollector(ParallelCollectors.parallel(superToSub));
            expectCollector(ParallelCollectors.parallel(objToSub));

            // ---------- parallelBy ----------
            expectCollector(ParallelCollectors.parallelBy(x -> x, superToSub, r -> {}, 42));
            expectCollector(ParallelCollectors.parallelBy(x -> x, objToSub, r -> {}, 42));

            expectCollector(ParallelCollectors.parallelBy(x -> x, superToSub, r -> {}));
            expectCollector(ParallelCollectors.parallelBy(x -> x, objToSub, r -> {}));

            expectCollector(ParallelCollectors.parallelBy(x -> x, superToSub, 42));
            expectCollector(ParallelCollectors.parallelBy(x -> x, objToSub, 42));

            expectCollector(ParallelCollectors.parallelBy(x -> x, superToSub));
            expectCollector(ParallelCollectors.parallelBy(x -> x, objToSub));

            // ---------- parallel (toList) ----------
            expectCollector(ParallelCollectors.parallel(superToSub, toList(), r -> {}, 42));
            expectCollector(ParallelCollectors.parallel(objToSub, toList(), r -> {}, 42));

            expectCollector(ParallelCollectors.parallel(superToSub, toList(), r -> {}));
            expectCollector(ParallelCollectors.parallel(objToSub, toList(), r -> {}));

            expectCollector(ParallelCollectors.parallel(superToSub, toList(), 42));
            expectCollector(ParallelCollectors.parallel(objToSub, toList(), 42));

            expectCollector(ParallelCollectors.parallel(superToSub, toList()));
            expectCollector(ParallelCollectors.parallel(objToSub, toList()));

            // ---------- parallelBy (toList) ----------
            expectCollector(ParallelCollectors.parallelBy(x -> x, superToSub, toList(), r -> {}, 42));
            expectCollector(ParallelCollectors.parallelBy(x -> x, objToSub, toList(), r -> {}, 42));

            expectCollector(ParallelCollectors.parallelBy(x -> x, superToSub, toList(), r -> {}));
            expectCollector(ParallelCollectors.parallelBy(x -> x, objToSub, toList(), r -> {}));

            expectCollector(ParallelCollectors.parallelBy(x -> x, superToSub, toList(), 42));
            expectCollector(ParallelCollectors.parallelBy(x -> x, objToSub, toList(), 42));

            expectCollector(ParallelCollectors.parallelBy(x -> x, superToSub, toList()));
            expectCollector(ParallelCollectors.parallelBy(x -> x, objToSub, toList()));

            // ---------- streaming ----------
            expectCollector(ParallelCollectors.parallelToStream(superToSub, r -> {}, 42));
            expectCollector(ParallelCollectors.parallelToStream(objToSub, r -> {}, 42));

            expectCollector(ParallelCollectors.parallelToStream(superToSub, r -> {}));
            expectCollector(ParallelCollectors.parallelToStream(objToSub, r -> {}));

            expectCollector(ParallelCollectors.parallelToOrderedStream(superToSub, r -> {}, 42));
            expectCollector(ParallelCollectors.parallelToOrderedStream(objToSub, r -> {}, 42));

            expectCollector(ParallelCollectors.parallelToOrderedStream(superToSub, r -> {}));
            expectCollector(ParallelCollectors.parallelToOrderedStream(objToSub, r -> {}));

            // ---------- streamingBy ----------
            expectCollector(ParallelCollectors.parallelToStreamBy(x -> x, superToSub, r -> {}, 42));
            expectCollector(ParallelCollectors.parallelToStreamBy(x -> x, objToSub, r -> {}, 42));

            expectCollector(ParallelCollectors.parallelToStreamBy(x -> x, superToSub, r -> {}));
            expectCollector(ParallelCollectors.parallelToStreamBy(x -> x, objToSub, r -> {}));

            expectCollector(ParallelCollectors.parallelToOrderedStreamBy(x -> x, superToSub, r -> {}, 42));
            expectCollector(ParallelCollectors.parallelToOrderedStreamBy(x -> x, objToSub, r -> {}, 42));

            expectCollector(ParallelCollectors.parallelToOrderedStreamBy(x -> x, superToSub, r -> {}));
            expectCollector(ParallelCollectors.parallelToOrderedStreamBy(x -> x, objToSub, r -> {}));
        }
    }
}
