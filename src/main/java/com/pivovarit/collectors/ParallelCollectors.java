package com.pivovarit.collectors;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.Option.batched;
import static com.pivovarit.collectors.Option.executor;
import static com.pivovarit.collectors.Option.parallelism;

/**
 * An umbrella class exposing static factory methods for instantiating parallel {@link Collector}s
 *
 * @author Grzegorz Piwowarek
 */
public final class ParallelCollectors {

    private ParallelCollectors() {
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations using Virtual Threads
     * and returning them as a {@link CompletableFuture} containing a result of the application of the user-provided {@link Collector}.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo(i), toList()));
     * }</pre>
     *
     * @param mapper    a transformation to be performed in parallel
     * @param collector the {@code Collector} describing the reduction
     * @param <T>       the type of the collected elements
     * @param <R>       the result returned by {@code mapper}
     * @param <RR>      the reduction result {@code collector}
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 3.0.0
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(Function<? super T, ? extends R> mapper, Collector<R, ?, RR> collector) {
        return AsyncParallelCollector.collecting(s -> s.collect(collector), mapper);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations using Virtual Threads
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
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     * @param <RR>        the reduction result {@code collector}
     * @param parallelism the max parallelism level
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 3.2.0
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(Function<? super T, ? extends R> mapper, Collector<R, ?, RR> collector, int parallelism) {
        return AsyncParallelCollector.collecting(s -> s.collect(collector), mapper, parallelism(parallelism));
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
     * @param <RR>        the reduction result {@code collector}
     * @param parallelism the max parallelism level
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 2.0.0
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(Function<? super T, ? extends R> mapper, Collector<R, ?, RR> collector, Executor executor, int parallelism) {
        return AsyncParallelCollector.collecting(s -> s.collect(collector), mapper, executor(executor), parallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor} with unlimited parallelism
     * and returning them as a {@link CompletableFuture} containing a result of the application of the user-provided {@link Collector}.
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
     * @param <RR>      the reduction result {@code collector}
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 3.3.0
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(Function<? super T, ? extends R> mapper, Collector<R, ?, RR> collector, Executor executor) {
        return AsyncParallelCollector.collecting(s -> s.collect(collector), mapper, executor(executor));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations using Virtual Threads
     * and returning them as {@link CompletableFuture} containing a {@link Stream} of these elements.
     *
     * <br><br>
     * The collector maintains the order of processed {@link Stream}. Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo()));
     * }</pre>
     *
     * @param mapper a transformation to be performed in parallel
     * @param <T>    the type of the collected elements
     * @param <R>    the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 3.0.0
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(Function<? super T, ? extends R> mapper) {
        return AsyncParallelCollector.collecting(mapper);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations using Virtual Threads
     * and returning them as {@link CompletableFuture} containing a {@link Stream} of these elements.
     *
     * <br><br>
     * The collector maintains the order of processed {@link Stream}. Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo()));
     * }</pre>
     *
     * @param mapper      a transformation to be performed in parallel
     * @param parallelism the max parallelism level
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 3.2.0
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(Function<? super T, ? extends R> mapper, int parallelism) {
        return AsyncParallelCollector.collecting(mapper, parallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Stream} of these elements.
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
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(Function<? super T, ? extends R> mapper, Executor executor, int parallelism) {
        return AsyncParallelCollector.collecting(mapper, executor(executor), parallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor} with unlimited parallelism
     * and returning them as {@link CompletableFuture} containing a {@link Stream} of these elements.
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
     * @since 3.3.0
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(Function<? super T, ? extends R> mapper, Executor executor) {
        return AsyncParallelCollector.collecting(mapper, executor(executor));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations using Virtual Threads
     * and returning a {@link Stream} instance returning results as they arrive.
     * <p>
     * For the parallelism of 1, the stream is executed by the calling thread.
     *
     * <br>
     * Example:
     * <pre>{@code
     * Stream.of(1, 2, 3)
     *   .collect(parallelToStream(i -> foo()))
     *   .forEach(System.out::println);
     * }</pre>
     *
     * @param mapper a transformation to be performed in parallel
     * @param <T>    the type of the collected elements
     * @param <R>    the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 3.0.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(Function<? super T, ? extends R> mapper) {
        return AsyncParallelStreamingCollector.streaming(mapper, false);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations using Virtual Threads
     * and returning a {@link Stream} instance returning results as they arrive.
     * <p>
     * For the parallelism of 1, the stream is executed by the calling thread.
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
     * @param parallelism the max parallelism level
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 3.2.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(Function<? super T, ? extends R> mapper, int parallelism) {
        return AsyncParallelStreamingCollector.streaming(mapper, false, parallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor} with unlimited parallelism
     * and returning a {@link Stream} instance returning results as they arrive.
     * <p>
     * For the parallelism of 1, the stream is executed by the calling thread.
     *
     * <br>
     * Example:
     * <pre>{@code
     * Stream.of(1, 2, 3)
     *   .collect(parallelToStream(i -> foo(), executor, 2))
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
     * @since 3.3.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(Function<? super T, ? extends R> mapper, Executor executor) {
        return AsyncParallelStreamingCollector.streaming(mapper, false, executor(executor));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning a {@link Stream} instance returning results as they arrive.
     * <p>
     * For the parallelism of 1, the stream is executed by the calling thread.
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
    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(Function<? super T, ? extends R> mapper, Executor executor, int parallelism) {
        return AsyncParallelStreamingCollector.streaming(mapper, false, executor(executor), parallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations using Virtual Threads
     * and returning a {@link Stream} instance returning results as they arrive while maintaining the initial order.
     * <p>
     * For the parallelism of 1, the stream is executed by the calling thread.
     *
     * <br>
     * Example:
     * <pre>{@code
     * Stream.of(1, 2, 3)
     *   .collect(parallelToOrderedStream(i -> foo()))
     *   .forEach(System.out::println);
     * }</pre>
     *
     * @param mapper a transformation to be performed in parallel
     * @param <T>    the type of the collected elements
     * @param <R>    the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 3.0.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToOrderedStream(Function<? super T, ? extends R> mapper) {
        return AsyncParallelStreamingCollector.streaming(mapper, true);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations using Virtual Threads
     * and returning a {@link Stream} instance returning results as they arrive while maintaining the initial order.
     * <p>
     * For the parallelism of 1, the stream is executed by the calling thread.
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
     * @param parallelism the max parallelism level
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 3.2.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToOrderedStream(Function<? super T, ? extends R> mapper, int parallelism) {
        return AsyncParallelStreamingCollector.streaming(mapper, true, parallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor} with unlimited parallelism
     * and returning a {@link Stream} instance returning results as they arrive while maintaining the initial order.
     * <p>
     * For the parallelism of 1, the stream is executed by the calling thread.
     *
     * <br>
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
     * @since 3.3.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToOrderedStream(Function<? super T, ? extends R> mapper, Executor executor) {
        return AsyncParallelStreamingCollector.streaming(mapper, true, executor(executor));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning a {@link Stream} instance returning results as they arrive while maintaining the initial order.
     * <p>
     * For the parallelism of 1, the stream is executed by the calling thread.
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
    public static <T, R> Collector<T, ?, Stream<R>> parallelToOrderedStream(Function<? super T, ? extends R> mapper, Executor executor, int parallelism) {
        return AsyncParallelStreamingCollector.streaming(mapper, true, executor(executor), parallelism(parallelism));
    }

    /**
     * A convenience {@code Collector} for collecting a {@code Stream<CompletableFuture<T>>}
     * into a {@code CompletableFuture<R>} using a provided {@code Collector<T, ?, R>}
     *
     * @param collector the {@code Collector} describing the reduction
     * @param <T>       the type of the collected elements
     * @param <R>       the result of the transformation
     *
     * @return a {@code Collector} which collects all futures and combines them into a single future
     * using the provided downstream {@code Collector}
     *
     * @since 2.3.0
     */
    public static <T, R> Collector<CompletableFuture<T>, ?, CompletableFuture<R>> toFuture(Collector<T, ?, R> collector) {
        return FutureCollectors.toFuture(collector);
    }

    /**
     * A convenience {@code Collector} for collecting a {@code Stream<CompletableFuture<T>>} into a {@code CompletableFuture<List<T>>}
     *
     * @param <T> the type of the collected elements
     *
     * @return a {@code Collector} which collects all futures and combines them into a single future
     * returning a list of results
     *
     * @since 2.3.0
     */
    public static <T> Collector<CompletableFuture<T>, ?, CompletableFuture<List<T>>> toFuture() {
        return FutureCollectors.toFuture();
    }

    /**
     * A subset of collectors which perform operations in batches and not separately (one object in a thread pool's worker queue represents a batch of operations to be performed by a single thread)
     */
    public static final class Batching {

        private Batching() {
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
         * @param parallelism the max parallelism level
         * @param <T>         the type of the collected elements
         * @param <R>         the result returned by {@code mapper}
         * @param <RR>        the reduction result {@code collector}
         *
         * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
         *
         * @since 2.1.0
         */
        public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(Function<? super T, ? extends R> mapper, Collector<R, ?, RR> collector, Executor executor, int parallelism) {
            return AsyncParallelCollector.collecting(s -> s.collect(collector), mapper,
              batched(),
              executor(executor),
              parallelism(parallelism));
        }

        /**
         * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
         * and returning them as {@link CompletableFuture} containing a {@link Stream} of these elements.
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
        public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(Function<? super T, ? extends R> mapper, Executor executor, int parallelism) {
            return AsyncParallelCollector.collecting(mapper,
              batched(),
              executor(executor),
              parallelism(parallelism));
        }

        /**
         * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
         * and returning a {@link Stream} instance returning results as they arrive.
         * <p>
         * For the parallelism of 1, the stream is executed by the calling thread.
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
        public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(Function<? super T, ? extends R> mapper, Executor executor, int parallelism) {
            return AsyncParallelStreamingCollector.streaming(mapper, false,
              batched(),
              executor(executor),
              parallelism(parallelism));
        }

        /**
         * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
         * and returning a {@link Stream} instance returning results as they arrive while maintaining the initial order.
         * <p>
         * For the parallelism of 1, the stream is executed by the calling thread.
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
         * @since 2.1.0
         */
        public static <T, R> Collector<T, ?, Stream<R>> parallelToOrderedStream(Function<? super T, ? extends R> mapper, Executor executor, int parallelism) {
            return AsyncParallelStreamingCollector.streaming(mapper, true,
              batched(),
              executor(executor),
              parallelism(parallelism));
        }
    }
}
