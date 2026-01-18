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
package com.pivovarit.collectors.test;

import com.pivovarit.collectors.ParallelCollectors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static com.pivovarit.collectors.test.Factory.GenericCollector.executorCollector;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class ExecutorValidationTest {

    private static Stream<Factory.GenericCollector<Factory.CollectorFactoryWithExecutor<Integer, Integer>>> allWithCustomExecutors() {
        return Stream.of(
          executorCollector("parallel(e)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e)), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallel(e, p=1)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(1)), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallel(e, p=4)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(4)), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallel(e, p=1) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(1).batching()), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallel(e, p=4) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, c -> c.executor(e).parallelism(14).batching()), c -> c.thenApply(Stream::toList).join())),
          executorCollector("parallel(toList(), e)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), c -> c.executor(e)), c -> c.join())),
          executorCollector("parallel(toList(), e, p=1)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), c -> c.executor(e).parallelism(1)), c -> c.join())),
          executorCollector("parallel(toList(), e, p=4)", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), c -> c.executor(e).parallelism(4)), c -> c.join())),
          executorCollector("parallel(toList(), e, p=1) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), c -> c.executor(e).parallelism(1).batching()), c -> c.join())),
          executorCollector("parallel(toList(), e, p=4) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.parallel(f, toList(), c -> c.executor(e).parallelism(4).batching()), c -> c.join())),
          executorCollector("parallelToStream(e)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e)), Stream::toList)),
          executorCollector("parallelToStream(e, p=1)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(1)), Stream::toList)),
          executorCollector("parallelToStream(e, p=4)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(4)), Stream::toList)),
          executorCollector("parallelToStream(e, p=1) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(1).batching()), Stream::toList)),
          executorCollector("parallelToStream(e, p=4) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(4).batching()), Stream::toList)),
          executorCollector("parallelToOrderedStream(e, p=1)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(1).ordered()), Stream::toList)),
          executorCollector("parallelToOrderedStream(e, p=4)", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(4).ordered()), Stream::toList)),
          executorCollector("parallelToOrderedStream(e, p=1) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(1).batching().ordered()), Stream::toList)),
          executorCollector("parallelToOrderedStream(e, p=4) [batching]", (f, e) -> collectingAndThen(ParallelCollectors.parallelToStream(f, c -> c.executor(e).parallelism(4).batching().ordered()), Stream::toList))
        );
    }

    @TestFactory
    Stream<DynamicTest> shouldRejectInvalidRejectedExecutionHandlerFactory() {
        return allWithCustomExecutors()
          .flatMap(c -> Stream.of(new ThreadPoolExecutor.DiscardOldestPolicy(), new ThreadPoolExecutor.DiscardPolicy())
            .map(dp -> DynamicTest.dynamicTest("%s : %s".formatted(c.name(), dp.getClass().getSimpleName()), () -> {
                try (var e = new ThreadPoolExecutor(2, 2000, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), dp)) {
                    assertThatThrownBy(() -> Stream.of(1, 2, 3).collect(c.factory().collector(i -> i, e))).isExactlyInstanceOf(IllegalArgumentException.class);
                }
            })));
    }
}
