package com.pivovarit.collectors;

import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.stream.Collectors.toList;

/**
 * This file exists solely to check that the public API exposes correct generic bounds (covariance /
 * contravariance). It must compile. It is not intended to run.
 */
@SuppressWarnings({"unused"})
final class TypeChecks {

  private TypeChecks() {}

  static class SuperClass {}

  static class SubClass extends SuperClass {}

  private static <A, R> void expectCollector(Collector<A, ?, R> c) {
    // compile-only
  }

  static final class Covariance {

    record Functions(
        Function<SubClass, SuperClass> subToSuper, Function<SubClass, SubClass> subToSub) {}

    private static final Functions fns = new Functions(x -> x, x -> x);

    record BatchingParallel(Functions f) {
      BatchingParallel {
        expectCollector(ParallelCollectors.Batching.parallel(f.subToSuper(), r -> {}, 42));
        expectCollector(ParallelCollectors.Batching.parallel(f.subToSub(), r -> {}, 42));
      }
    }

    record BatchingParallelToList(Functions f) {
      BatchingParallelToList {
        expectCollector(
            ParallelCollectors.Batching.parallel(f.subToSuper(), toList(), r -> {}, 42));
        expectCollector(ParallelCollectors.Batching.parallel(f.subToSub(), toList(), r -> {}, 42));
      }
    }

    record BatchingParallelToStream(Functions f) {
      BatchingParallelToStream {
        expectCollector(ParallelCollectors.Batching.parallelToStream(f.subToSuper(), r -> {}, 42));
        expectCollector(ParallelCollectors.Batching.parallelToStream(f.subToSub(), r -> {}, 42));

        expectCollector(
            ParallelCollectors.Batching.parallelToOrderedStream(f.subToSuper(), r -> {}, 42));
        expectCollector(
            ParallelCollectors.Batching.parallelToOrderedStream(f.subToSub(), r -> {}, 42));
      }
    }

    record Parallel(Functions f) {
      Parallel {
        expectCollector(ParallelCollectors.parallel(f.subToSuper(), r -> {}, 42));
        expectCollector(ParallelCollectors.parallel(f.subToSub(), r -> {}, 42));

        expectCollector(ParallelCollectors.parallel(f.subToSuper(), r -> {}));
        expectCollector(ParallelCollectors.parallel(f.subToSub(), r -> {}));

        expectCollector(ParallelCollectors.parallel(f.subToSuper(), 42));
        expectCollector(ParallelCollectors.parallel(f.subToSub(), 42));

        expectCollector(ParallelCollectors.parallel(f.subToSuper()));
        expectCollector(ParallelCollectors.parallel(f.subToSub()));
      }
    }

    record ParallelBy(Functions f) {
      ParallelBy {
        expectCollector(ParallelCollectors.parallelBy(f.subToSuper(), f.subToSuper(), r -> {}, 42));
        expectCollector(ParallelCollectors.parallelBy(f.subToSuper(), f.subToSub(), r -> {}, 42));
        expectCollector(ParallelCollectors.parallelBy(f.subToSub(), f.subToSuper(), r -> {}, 42));
        expectCollector(ParallelCollectors.parallelBy(f.subToSub(), f.subToSub(), r -> {}, 42));
      }
    }

    record ParallelToList(Functions f) {
      ParallelToList {
        expectCollector(ParallelCollectors.parallel(f.subToSuper(), toList(), r -> {}, 42));
        expectCollector(ParallelCollectors.parallel(f.subToSub(), toList(), r -> {}, 42));
      }
    }

    record ParallelByToList(Functions f) {
      ParallelByToList {
        expectCollector(
            ParallelCollectors.parallelBy(f.subToSuper(), f.subToSuper(), toList(), r -> {}, 42));
        expectCollector(
            ParallelCollectors.parallelBy(f.subToSuper(), f.subToSub(), toList(), r -> {}, 42));
        expectCollector(
            ParallelCollectors.parallelBy(f.subToSub(), f.subToSuper(), toList(), r -> {}, 42));
        expectCollector(
            ParallelCollectors.parallelBy(f.subToSub(), f.subToSub(), toList(), r -> {}, 42));
      }
    }

    record Streaming(Functions f) {
      Streaming {
        expectCollector(ParallelCollectors.parallelToStream(f.subToSuper(), r -> {}, 42));
        expectCollector(ParallelCollectors.parallelToStream(f.subToSub(), r -> {}, 42));

        expectCollector(ParallelCollectors.parallelToOrderedStream(f.subToSuper(), r -> {}, 42));
        expectCollector(ParallelCollectors.parallelToOrderedStream(f.subToSub(), r -> {}, 42));
      }
    }

    record StreamingBy(Functions f) {
      StreamingBy {
        expectCollector(
            ParallelCollectors.parallelToStreamBy(f.subToSuper(), f.subToSuper(), r -> {}, 42));
        expectCollector(
            ParallelCollectors.parallelToStreamBy(f.subToSuper(), f.subToSub(), r -> {}, 42));
        expectCollector(
            ParallelCollectors.parallelToStreamBy(f.subToSub(), f.subToSuper(), r -> {}, 42));
        expectCollector(
            ParallelCollectors.parallelToStreamBy(f.subToSub(), f.subToSub(), r -> {}, 42));

        expectCollector(
            ParallelCollectors.parallelToOrderedStreamBy(
                f.subToSuper(), f.subToSuper(), r -> {}, 42));
        expectCollector(
            ParallelCollectors.parallelToOrderedStreamBy(
                f.subToSuper(), f.subToSub(), r -> {}, 42));
        expectCollector(
            ParallelCollectors.parallelToOrderedStreamBy(
                f.subToSub(), f.subToSuper(), r -> {}, 42));
        expectCollector(
            ParallelCollectors.parallelToOrderedStreamBy(f.subToSub(), f.subToSub(), r -> {}, 42));
      }
    }
  }

  static final class Contravariance {

    private static final Function<SuperClass, SubClass> superToSub = x -> new SubClass();
    private static final Function<Object, SubClass> objToSub = x -> new SubClass();

    record BatchingParallel() {
      BatchingParallel {
        expectCollector(ParallelCollectors.Batching.parallel(superToSub, r -> {}, 42));
        expectCollector(ParallelCollectors.Batching.parallel(objToSub, r -> {}, 42));
      }
    }

    record BatchingParallelToList() {
      BatchingParallelToList {
        expectCollector(ParallelCollectors.Batching.parallel(superToSub, toList(), r -> {}, 42));
        expectCollector(ParallelCollectors.Batching.parallel(objToSub, toList(), r -> {}, 42));
      }
    }

    record BatchingParallelToStream() {
      BatchingParallelToStream {
        expectCollector(ParallelCollectors.Batching.parallelToStream(superToSub, r -> {}, 42));
        expectCollector(ParallelCollectors.Batching.parallelToStream(objToSub, r -> {}, 42));

        expectCollector(
            ParallelCollectors.Batching.parallelToOrderedStream(superToSub, r -> {}, 42));
        expectCollector(ParallelCollectors.Batching.parallelToOrderedStream(objToSub, r -> {}, 42));
      }
    }

    record Parallel() {
      Parallel {
        expectCollector(ParallelCollectors.parallel(superToSub, r -> {}, 42));
        expectCollector(ParallelCollectors.parallel(objToSub, r -> {}, 42));

        expectCollector(ParallelCollectors.parallel(superToSub, r -> {}));
        expectCollector(ParallelCollectors.parallel(objToSub, r -> {}));

        expectCollector(ParallelCollectors.parallel(superToSub, 42));
        expectCollector(ParallelCollectors.parallel(objToSub, 42));

        expectCollector(ParallelCollectors.parallel(superToSub));
        expectCollector(ParallelCollectors.parallel(objToSub));
      }
    }

    record ParallelBy() {
      ParallelBy {
        expectCollector(ParallelCollectors.parallelBy(superToSub, superToSub, r -> {}, 42));
        expectCollector(ParallelCollectors.parallelBy(superToSub, objToSub, r -> {}, 42));
        expectCollector(ParallelCollectors.parallelBy(objToSub, superToSub, r -> {}, 42));
        expectCollector(ParallelCollectors.parallelBy(objToSub, objToSub, r -> {}, 42));
      }
    }

    record ParallelToList() {
      ParallelToList {
        expectCollector(ParallelCollectors.parallel(superToSub, toList(), r -> {}, 42));
        expectCollector(ParallelCollectors.parallel(objToSub, toList(), r -> {}, 42));
      }
    }

    record ParallelByToList() {
      ParallelByToList {
        expectCollector(
            ParallelCollectors.parallelBy(superToSub, superToSub, toList(), r -> {}, 42));
        expectCollector(ParallelCollectors.parallelBy(superToSub, objToSub, toList(), r -> {}, 42));
        expectCollector(ParallelCollectors.parallelBy(objToSub, superToSub, toList(), r -> {}, 42));
        expectCollector(ParallelCollectors.parallelBy(objToSub, objToSub, toList(), r -> {}, 42));
      }
    }

    record Streaming() {
      Streaming {
        expectCollector(ParallelCollectors.parallelToStream(superToSub, r -> {}, 42));
        expectCollector(ParallelCollectors.parallelToStream(objToSub, r -> {}, 42));

        expectCollector(ParallelCollectors.parallelToOrderedStream(superToSub, r -> {}, 42));
        expectCollector(ParallelCollectors.parallelToOrderedStream(objToSub, r -> {}, 42));
      }
    }

    record StreamingBy() {
      StreamingBy {
        expectCollector(ParallelCollectors.parallelToStreamBy(superToSub, superToSub, r -> {}, 42));
        expectCollector(ParallelCollectors.parallelToStreamBy(superToSub, objToSub, r -> {}, 42));
        expectCollector(ParallelCollectors.parallelToStreamBy(objToSub, superToSub, r -> {}, 42));
        expectCollector(ParallelCollectors.parallelToStreamBy(objToSub, objToSub, r -> {}, 42));

        expectCollector(
            ParallelCollectors.parallelToOrderedStreamBy(superToSub, superToSub, r -> {}, 42));
        expectCollector(
            ParallelCollectors.parallelToOrderedStreamBy(superToSub, objToSub, r -> {}, 42));
        expectCollector(
            ParallelCollectors.parallelToOrderedStreamBy(objToSub, superToSub, r -> {}, 42));
        expectCollector(
            ParallelCollectors.parallelToOrderedStreamBy(objToSub, objToSub, r -> {}, 42));
      }
    }
  }
}
