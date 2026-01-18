/*
 * Copyright 2014-2026 Grzegorz Piwowarek, https://4comprehension.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    static final class Covariance {

        record Functions(
          Function<SubClass, SuperClass> subToSuper,
          Function<SubClass, SubClass> subToSub
        ) {
        }

        private static final Functions fns =
          new Functions(x -> x, x -> x);

        record Parallel(Functions f) {
            Parallel {
                expectCollector(ParallelCollectors.parallel(f.subToSuper, r -> {}));
                expectCollector(ParallelCollectors.parallel(f.subToSub, r -> {}));

                expectCollector(ParallelCollectors.parallel(f.subToSuper, 42));
                expectCollector(ParallelCollectors.parallel(f.subToSub, 42));

                expectCollector(ParallelCollectors.parallel(f.subToSuper));
                expectCollector(ParallelCollectors.parallel(f.subToSub));
            }
        }

        record ParallelBy(Functions f) {
            ParallelBy {
                expectCollector(ParallelCollectors.parallelBy(f.subToSuper, f.subToSuper, c -> {}));
                expectCollector(ParallelCollectors.parallelBy(f.subToSuper, f.subToSub, c -> {}));
                expectCollector(ParallelCollectors.parallelBy(f.subToSub, f.subToSuper, c -> {}));
                expectCollector(ParallelCollectors.parallelBy(f.subToSub, f.subToSub, c -> {}));
            }
        }

        record ParallelToList(Functions f) {
            ParallelToList {
                expectCollector(ParallelCollectors.parallel(f.subToSuper, toList(), c -> {}));
                expectCollector(ParallelCollectors.parallel(f.subToSub, toList(), c -> {}));
            }
        }

        record ParallelByToList(Functions f) {
            ParallelByToList {
                expectCollector(ParallelCollectors.parallelBy(f.subToSuper, f.subToSuper, toList(), c -> {}));
                expectCollector(ParallelCollectors.parallelBy(f.subToSuper, f.subToSub, toList(), c -> {}));
                expectCollector(ParallelCollectors.parallelBy(f.subToSub, f.subToSuper, toList(), c -> {}));
                expectCollector(ParallelCollectors.parallelBy(f.subToSub, f.subToSub, toList(), c -> {}));
            }
        }

        record Streaming(Functions f) {
            Streaming {
                expectCollector(ParallelCollectors.parallelToStream(f.subToSuper, c -> {}));
                expectCollector(ParallelCollectors.parallelToStream(f.subToSub, c -> {}));
            }
        }

        record StreamingBy(Functions f) {
            StreamingBy {
                expectCollector(ParallelCollectors.parallelToStreamBy(f.subToSuper, f.subToSuper, c -> {}));
                expectCollector(ParallelCollectors.parallelToStreamBy(f.subToSuper, f.subToSub, c -> {}));
                expectCollector(ParallelCollectors.parallelToStreamBy(f.subToSub, f.subToSuper, c -> {}));
                expectCollector(ParallelCollectors.parallelToStreamBy(f.subToSub, f.subToSub, c -> {}));
            }
        }
    }

    static final class Contravariance {

        private static final Function<SuperClass, SubClass> superToSub = x -> new SubClass();
        private static final Function<Object, SubClass> objToSub = x -> new SubClass();

        record Parallel() {
            Parallel {
                expectCollector(ParallelCollectors.parallel(superToSub, c -> {}));
                expectCollector(ParallelCollectors.parallel(objToSub, c -> {}));

                expectCollector(ParallelCollectors.parallel(superToSub, 42));
                expectCollector(ParallelCollectors.parallel(objToSub, 42));

                expectCollector(ParallelCollectors.parallel(superToSub));
                expectCollector(ParallelCollectors.parallel(objToSub));
            }
        }

        record ParallelBy() {
            ParallelBy {
                expectCollector(ParallelCollectors.parallelBy(superToSub, superToSub, c -> {}));
                expectCollector(ParallelCollectors.parallelBy(superToSub, objToSub, c -> {}));
                expectCollector(ParallelCollectors.parallelBy(objToSub, superToSub, c -> {}));
                expectCollector(ParallelCollectors.parallelBy(objToSub, objToSub, c -> {}));
            }
        }

        record ParallelToList() {
            ParallelToList {
                expectCollector(ParallelCollectors.parallel(superToSub, toList(), c -> {}));
                expectCollector(ParallelCollectors.parallel(objToSub, toList(), c -> {}));
            }
        }

        record ParallelByToList() {
            ParallelByToList {
                expectCollector(ParallelCollectors.parallelBy(superToSub, superToSub, toList(), c -> {}));
                expectCollector(ParallelCollectors.parallelBy(superToSub, objToSub, toList(), c -> {}));
                expectCollector(ParallelCollectors.parallelBy(objToSub, superToSub, toList(), c -> {}));
                expectCollector(ParallelCollectors.parallelBy(objToSub, objToSub, toList(), c -> {}));
            }
        }

        record Streaming() {
            Streaming {
                expectCollector(ParallelCollectors.parallelToStream(superToSub, c -> {}));
                expectCollector(ParallelCollectors.parallelToStream(objToSub, c -> {}));
            }
        }

        record StreamingBy() {
            StreamingBy {
                expectCollector(ParallelCollectors.parallelToStreamBy(superToSub, superToSub, c -> {}));
                expectCollector(ParallelCollectors.parallelToStreamBy(superToSub, objToSub, c -> {}));
                expectCollector(ParallelCollectors.parallelToStreamBy(objToSub, superToSub, c -> {}));
                expectCollector(ParallelCollectors.parallelToStreamBy(objToSub, objToSub, c -> {}));
            }
        }
    }
}
