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
     * The parallelism level defaults to {@code Runtime.availableProcessors() - 1}
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(toList(), i -> foo(i), executor));
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
     * @since 1.2.0
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor) {
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
     *   .collect(parallel(toList(), i -> foo(i), executor, 2));
     * }</pre>
     *
     * @param mapper      a transformation to be performed in parallel
     * @param collector   the {@code Collector} describing the reduction
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     * @param parallelism the parallelism level
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 1.2.0
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor, int parallelism) {
        return collectingWithCollector(collector, mapper, executor, parallelism);
    }


    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Stream} of these elements
     *
     * <br><br>
     * The parallelism level defaults to {@code Runtime.availableProcessors() - 1}
     *
     * <br><br>
     * The collector maintains the order of processed {@link Stream}. Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToStream(i -> foo(), executor));
     * }</pre>
     *
     * @param mapper   a transformation to be performed in parallel
     * @param executor the {@code Executor} to use for asynchronous execution
     * @param <T>      the type of the collected elements
     * @param <R>      the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 0.3.0
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(Function<T, R> mapper, Executor executor) {
        return AsyncParallelCollector.collectingToStream(mapper, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Stream} of these elements.
     *
     * <br><br>
     * The parallelism level defaults to {@code Runtime.availableProcessors() - 1}
     *
     * <br><br>
     * The collector maintains the order of processed {@link Stream}. Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToStream(i -> foo(), executor, 2));
     * }</pre>
     *
     * @param mapper      a transformation to be performed in parallel
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param parallelism the parallelism level
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 0.3.0
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(Function<T, R> mapper, Executor executor, int parallelism) {
        return AsyncParallelCollector.collectingToStream(mapper, executor, parallelism);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning a {@link Stream} instance returning results in completion order
     *
     * <br><br>
     * The parallelism level defaults to {@code Runtime.availableProcessors() - 1}
     *
     * <br><br>
     * Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * List<String> result = Stream.of(1, 2, 3)
     *   .collect(parallelMap(i -> foo(), executor))
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
     * @since 1.1.0
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
     * List<String> result = Stream.of(1, 2, 3)
     *   .collect(parallelMap(i -> foo(), executor, 2))
     *   .collect(toList());
     * }</pre>
     *
     * @param mapper      a transformation to be performed in parallel
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param parallelism the parallelism level
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 1.1.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(Function<T, R> mapper, Executor executor, int parallelism) {
        return ParallelStreamCollector.streaming(mapper, executor, parallelism);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning a {@link Stream} instance returning results as they arrive while maintaining the initial order.
     *
     * <br><br>
     * The parallelism level defaults to {@code Runtime.availableProcessors() - 1}
     *
     * <br><br>
     * Instances should not be reused.
     * <br><br>
     * Example:
     * <pre>{@code
     * List<String> result = Stream.of(1, 2, 3)
     *   .collect(parallelMapOrdered(i -> foo(), executor))
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
     * @since 1.1.0
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
     * List<String> result = Stream.of(1, 2, 3)
     *   .collect(parallelMapOrdered(i -> foo(), executor, 2))
     *   .collect(toList());
     * }</pre>
     *
     * @param mapper      a transformation to be performed in parallel
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param parallelism the parallelism level
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 1.1.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToOrderedStream(Function<T, R> mapper, Executor executor, int parallelism) {
        return ParallelStreamCollector.streamingOrdered(mapper, executor, parallelism);
    }
}
