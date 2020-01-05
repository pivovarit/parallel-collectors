package com.pivovarit.collectors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.AsyncParallelCollector.collectingWithCollector;

/**
 * An umbrella class exposing static factory methods for instantiating parallel {@link Collector}s
 *
 * @author Grzegorz Piwowarek
 */
public final class ParallelCollectors {

    private ParallelCollectors() {
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as a {@link CompletableFuture} containing a result of the application of the user-provided {@link Collector}.
     *
     * <br><br>
     * The maximum parallelism level defaults to {@code Runtime.availableProcessors() - 1}
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo(i), toList(), executor));
     * }</pre>
     *
     * @param mapper    a transformation to be performed in parallel
     * @param collector the {@code Collector} describing the reduction
     * @param executor  the {@code Executor} to use for asynchronous execution
     * @param <T>       the type of the collected elements
     * @param <R>       the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 2.0.0
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(Function<T, R> mapper, Collector<R, ?, RR> collector, Executor executor) {
        return collectingWithCollector(collector, mapper, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as a {@link CompletableFuture} containing a result of the application of the user-provided {@link Collector}.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo(i), toList(), executor, 2));
     * }</pre>
     *
     * @param mapper      a transformation to be performed in parallel
     * @param collector   the {@code Collector} describing the reduction
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     * @param parallelism the max parallelism level
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 2.0.0
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(Function<T, R> mapper, Collector<R, ?, RR> collector, Executor executor, int parallelism) {
        return collectingWithCollector(collector, mapper, executor, parallelism);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Stream} of these elements
     *
     * <br><br>
     * The max parallelism level defaults to {@code Runtime.availableProcessors() - 1}
     *
     * <br><br>
     * The collector maintains the order of processed {@link Stream}. Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo(), executor));
     * }</pre>
     *
     * @param mapper   a transformation to be performed in parallel
     * @param executor the {@code Executor} to use for asynchronous execution
     * @param <T>      the type of the collected elements
     * @param <R>      the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 2.0.0
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(Function<T, R> mapper, Executor executor) {
        return AsyncParallelCollector.collectingToStream(mapper, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Stream} of these elements.
     *
     * <br><br>
     * The max parallelism level defaults to {@code Runtime.availableProcessors() - 1}
     *
     * <br><br>
     * The collector maintains the order of processed {@link Stream}. Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo(), executor, 2));
     * }</pre>
     *
     * @param mapper      a transformation to be performed in parallel
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param parallelism the max parallelism level
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 2.0.0
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(Function<T, R> mapper, Executor executor, int parallelism) {
        return AsyncParallelCollector.collectingToStream(mapper, executor, parallelism);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning a {@link Stream} instance returning results in completion order
     *
     * <br><br>
     * The max parallelism level defaults to {@code Runtime.availableProcessors() - 1}
     *
     * <br><br>
     * Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * List<String> result = Stream.of(1, 2, 3)
     *   .collect(parallelToStream(i -> foo(), executor))
     *   .collect(toList());
     * }</pre>
     *
     * @param mapper   a transformation to be performed in parallel
     * @param executor the {@code Executor} to use for asynchronous execution
     * @param <T>      the type of the collected elements
     * @param <R>      the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 2.0.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(Function<T, R> mapper, Executor executor) {
        return ParallelStreamCollector.streaming(mapper, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning a {@link Stream} instance returning results as they arrive.
     *
     * <br>
     * Example:
     * <pre>{@code
     * Stream.of(1, 2, 3)
     *   .collect(parallelToStream(i -> foo(), executor, 2))
     *   .forEach(System.out::println);
     * }</pre>
     *
     * @param mapper      a transformation to be performed in parallel
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param parallelism the max parallelism level
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 2.0.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(Function<T, R> mapper, Executor executor, int parallelism) {
        return ParallelStreamCollector.streaming(mapper, executor, parallelism);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning a {@link Stream} instance returning results as they arrive while maintaining the initial order.
     *
     * <br><br>
     * The max parallelism level defaults to {@code Runtime.availableProcessors() - 1}
     *
     * <br><br>
     * Instances should not be reused.
     * <br><br>
     * Example:
     * <pre>{@code
     * Stream.of(1, 2, 3)
     *   .collect(parallelToOrderedStream(i -> foo(), executor))
     *   .forEach(System.out::println);
     * }</pre>
     *
     * @param mapper   a transformation to be performed in parallel
     * @param executor the {@code Executor} to use for asynchronous execution
     * @param <T>      the type of the collected elements
     * @param <R>      the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 2.0.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToOrderedStream(Function<T, R> mapper, Executor executor) {
        return ParallelStreamCollector.streamingOrdered(mapper, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning a {@link Stream} instance returning results as they arrive while maintaining the initial order.
     *
     * <br>
     * Example:
     * <pre>{@code
     * Stream.of(1, 2, 3)
     *   .collect(parallelToOrderedStream(i -> foo(), executor, 2))
     *   .forEach(System.out::println);
     * }</pre>
     *
     * @param mapper      a transformation to be performed in parallel
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param parallelism the max parallelism level
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 2.0.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToOrderedStream(Function<T, R> mapper, Executor executor, int parallelism) {
        return ParallelStreamCollector.streamingOrdered(mapper, executor, parallelism);
    }

    /**
     * A subset of collectors which perform operations in batches and not separately (one object in a thread pool's worker queue represents a batch of operations to be performed by a single thread)
     */
    static class Batching {

        /**
         * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
         * and returning them as a {@link CompletableFuture} containing a result of the application of the user-provided {@link Collector}.
         *
         * @implNote this collector processed elements in batches and not separately
         *
         * <br>
         * Example:
         * <pre>{@code
         * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
         *   .collect(parallel(i -> foo(i), toList(), executor, 2));
         * }</pre>
         *
         * @param mapper      a transformation to be performed in parallel
         * @param collector   the {@code Collector} describing the reduction
         * @param executor    the {@code Executor} to use for asynchronous execution
         * @param <T>         the type of the collected elements
         * @param <R>         the result returned by {@code mapper}
         * @param parallelism the max parallelism level
         *
         * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
         *
         * @since 2.1.0
         */
        public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(Function<T, R> mapper, Collector<R, ?, RR> collector, Executor executor, int parallelism) {
            return AsyncParallelCollector.Batching.collectingWithCollectorInBatches(collector, mapper, executor, parallelism);
        }

        /**
         * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
         * and returning them as {@link CompletableFuture} containing a {@link Stream} of these elements.
         *
         * @implNote this collector processed elements in batches and not separately
         *
         * <br><br>
         * The max parallelism level defaults to {@code Runtime.availableProcessors() - 1}
         *
         * <br><br>
         * The collector maintains the order of processed {@link Stream}. Instances should not be reused.
         *
         * <br>
         * Example:
         * <pre>{@code
         * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
         *   .collect(parallel(i -> foo(), executor, 2));
         * }</pre>
         *
         * @param mapper      a transformation to be performed in parallel
         * @param executor    the {@code Executor} to use for asynchronous execution
         * @param parallelism the max parallelism level
         * @param <T>         the type of the collected elements
         * @param <R>         the result returned by {@code mapper}
         *
         * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
         *
         * @since 2.1.0
         */
        public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(Function<T, R> mapper, Executor executor, int parallelism) {
            return AsyncParallelCollector.Batching.collectingToStreamInBatches(mapper, executor, parallelism);
        }


        /**
         * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
         * and returning a {@link Stream} instance returning results as they arrive.
         *
         * @implNote this collector processed elements in batches and not separately
         *
         * <br>
         * Example:
         * <pre>{@code
         * Stream.of(1, 2, 3)
         *   .collect(parallelToStream(i -> foo(), executor, 2))
         *   .forEach(System.out::println);
         * }</pre>
         *
         * @param mapper      a transformation to be performed in parallel
         * @param executor    the {@code Executor} to use for asynchronous execution
         * @param parallelism the max parallelism level
         * @param <T>         the type of the collected elements
         * @param <R>         the result returned by {@code mapper}
         *
         * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
         *
         * @since 2.1.0
         */
        public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(Function<T, R> mapper, Executor executor, int parallelism) {
            return ParallelStreamCollector.Batching.streamingInBatches(mapper, executor, parallelism);
        }

        /**
         * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
         * and returning a {@link Stream} instance returning results as they arrive while maintaining the initial order.
         *
         * @implNote this collector processed elements in batches and not separately
         *
         * <br>
         * Example:
         * <pre>{@code
         * Stream.of(1, 2, 3)
         *   .collect(parallelToOrderedStream(i -> foo(), executor, 2))
         *   .forEach(System.out::println);
         * }</pre>
         *
         * @param mapper      a transformation to be performed in parallel
         * @param executor    the {@code Executor} to use for asynchronous execution
         * @param parallelism the max parallelism level
         * @param <T>         the type of the collected elements
         * @param <R>         the result returned by {@code mapper}
         *
         * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
         *s
         * gigi
         * @since 2.1.0
         */
        public static <T, R> Collector<T, ?, Stream<R>> parallelToOrderedStream(Function<T, R> mapper, Executor executor, int parallelism) {
            return ParallelStreamCollector.Batching.streamingOrderedInBatches(mapper, executor, parallelism);
        }
    }
}
