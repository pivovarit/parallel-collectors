package com.pivovarit.collectors;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.AsyncParallelCollector.collectingWithCollector;
import static com.pivovarit.collectors.AsyncParallelCollector.defaultListSupplier;
import static com.pivovarit.collectors.AsyncParallelCollector.defaultSetSupplier;
import static java.util.stream.Collectors.toList;

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
     * and returning them as a {@link CompletableFuture} containing a user-provided {@link Collection} of these elements.
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
     * CompletableFuture<TreeSet<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToCollection(i -> foo(i), TreeSet::new, executor));
     * }</pre>
     *
     * @param mapper             a transformation to be performed in parallel
     * @param collectionSupplier a {@code Supplier} which returns a mutable {@code Collection} of the appropriate type
     * @param executor           the {@code Executor} to use for asynchronous execution
     * @param <T>                the type of the collected elements
     * @param <R>                the result returned by {@code mapper}
     * @param <C>                the collection supplier
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 0.0.1
     * @deprecated use {@link ParallelCollectors#parallel(Collector, Function, Executor)} )} instead
     */
    @Deprecated // for removal
    public static <T, R, C extends Collection<R>> Collector<T, ?, CompletableFuture<C>> parallelToCollection(Function<T, R> mapper, Supplier<C> collectionSupplier, Executor executor) {
        return AsyncParallelCollector.collectingToCollection(mapper, collectionSupplier, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a user-provided {@link Collection} {@link R} of these elements
     *
     * <br><br>
     * Encounter order is preserved. Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<TreeSet<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToCollection(i -> foo(i), TreeSet::new, executor, 2));
     * }</pre>
     *
     * @param mapper             a transformation to be performed in parallel
     * @param collectionSupplier a {@code Supplier} which returns a mutable {@code Collection} of the appropriate type
     * @param executor           the {@code Executor} to use for asynchronous execution
     * @param parallelism        the parallelism level
     * @param <T>                the type of the collected elements
     * @param <R>                the result returned by {@code mapper}
     * @param <C>                the collection supplier
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 0.0.1
     * @deprecated use {@link ParallelCollectors#parallel(Collector, Function, Executor, int)} )} instead
     */
    @Deprecated // for removal
    public static <T, R, C extends Collection<R>> Collector<T, ?, CompletableFuture<C>> parallelToCollection(Function<T, R> mapper, Supplier<C> collectionSupplier, Executor executor, int parallelism) {
        return AsyncParallelCollector.collectingToCollection(mapper, collectionSupplier, executor, parallelism);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Set} of these elements
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
     * CompletableFuture<Set<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToSet(i -> foo(), executor));
     * }</pre>
     *
     * @param mapper   a transformation to be performed in parallel
     * @param executor the {@code Executor} to use for asynchronous execution
     * @param <T>      the type of the collected elements
     * @param <R>      the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Set} in parallel
     *
     * @since 0.0.1
     * @deprecated use {@link ParallelCollectors#parallel(Collector, Function, Executor)} )} instead
     */
    @Deprecated // for removal
    public static <T, R> Collector<T, ?, CompletableFuture<Set<R>>> parallelToSet(Function<T, R> mapper, Executor executor) {
        return AsyncParallelCollector.collectingToCollection(mapper, defaultSetSupplier(), executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Set} of these elements
     *
     * <br><br>
     * Encounter order is preserved. Instances should not be reused.
     *
     * <br><br>
     * Warning: this implementation can't be used with infinite {@link java.util.stream.Stream} instances.
     * It will try to submit {@code N} tasks to a provided {@link Executor}
     * where {@code N} is a size of a collected {@link java.util.stream.Stream}
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Set<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToSet(i -> foo(), executor, 2));
     * }</pre>
     *
     * @param mapper      a transformation to be performed in parallel
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param parallelism the parallelism level
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Set} in parallel
     *
     * @since 0.0.1
     * @deprecated use {@link ParallelCollectors#parallel(Collector, Function, Executor, int)} )} instead
     */
    @Deprecated // for removal
    public static <T, R> Collector<T, ?, CompletableFuture<Set<R>>> parallelToSet(Function<T, R> mapper, Executor executor, int parallelism) {
        return AsyncParallelCollector.collectingToCollection(mapper, defaultSetSupplier(), executor, parallelism);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Map} of these elements
     *
     * <br><br>
     * The parallelism level defaults to {@code Runtime.availableProcessors() - 1}
     *
     * <br><br>
     * On duplicate key, completes exceptionally with {@link IllegalStateException}
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Map<Integer, Integer>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToMap(i -> i, i -> i * 2, executor));
     * }</pre>
     *
     * @param keyMapper   the key deriving operation to be performed in parallel
     * @param valueMapper the value deriving operation to be performed in parallel
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param <T>         the type of the collected elements
     * @param <K>         the target {@code Map} key type
     * @param <V>         the target {@code Map} value type
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Map} in parallel
     *
     * @since 0.2.0
     */
    public static <T, K, V> Collector<T, ?, CompletableFuture<Map<K, V>>> parallelToMap(Function<T, K> keyMapper, Function<T, V> valueMapper, Executor executor) {
        return AsyncParallelCollector.collectingToMap(keyMapper, valueMapper, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Map} of these elements
     *
     * <br><br>
     * Encounter order is preserved. Instances should not be reused.
     * <br><br>
     * On duplicate key, completes exceptionally with {@link IllegalStateException}
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Map<Integer, Integer>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToMap(i -> i, i -> i * 2, executor, 2));
     * }</pre>
     *
     * @param keyMapper   the key deriving operation to be performed in parallel
     * @param valueMapper the value deriving operation to be performed in parallel
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param parallelism the parallelism level
     * @param <T>         the type of the collected elements
     * @param <K>         the target {@code Map} key type
     * @param <V>         the target {@code Map} value type
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Map} in parallel
     *
     * @since 0.2.0
     */
    public static <T, K, V> Collector<T, ?, CompletableFuture<Map<K, V>>> parallelToMap(Function<T, K> keyMapper, Function<T, V> valueMapper, Executor executor, int parallelism) {
        return AsyncParallelCollector.collectingToMap(keyMapper, valueMapper, executor, parallelism);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Map} of these elements
     *
     * <br><br>
     * The parallelism level defaults to {@code Runtime.availableProcessors() - 1}
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Map<Integer, Integer>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToMap(i -> i, i -> i * 2, Integer::sum, executor));
     * }</pre>
     *
     * @param keyMapper   the key deriving operation to be performed in parallel
     * @param valueMapper the value deriving operation to be performed in parallel
     * @param merger      the duplicate key value resolution strategy
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param <T>         the type of the collected elements
     * @param <K>         the target {@code Map} key type
     * @param <V>         the target {@code Map} value type
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Map} in parallel
     *
     * @since 0.2.0
     */
    public static <T, K, V> Collector<T, ?, CompletableFuture<Map<K, V>>> parallelToMap(Function<T, K> keyMapper, Function<T, V> valueMapper, BinaryOperator<V> merger, Executor executor) {
        return AsyncParallelCollector.collectingToMap(keyMapper, valueMapper, merger, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Map} of these elements
     *
     * <br><br>
     * Encounter order is preserved. Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Map<Integer, Integer>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToMap(i -> i, i -> i * 2, Integer::sum, executor, 2));
     * }</pre>
     *
     * @param keyMapper   the key deriving operation to be performed in parallel
     * @param valueMapper the value deriving operation to be performed in parallel
     * @param merger      the duplicate key value resolution strategy
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param parallelism the parallelism level
     * @param <T>         the type of the collected elements
     * @param <K>         the target {@code Map} key type
     * @param <V>         the target {@code Map} value type
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Map} in parallel
     *
     * @since 0.2.0
     */
    public static <T, K, V> Collector<T, ?, CompletableFuture<Map<K, V>>> parallelToMap(Function<T, K> keyMapper, Function<T, V> valueMapper, BinaryOperator<V> merger, Executor executor, int parallelism) {
        return AsyncParallelCollector.collectingToMap(keyMapper, valueMapper, merger, executor, parallelism);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Map} of these elements
     *
     * <br><br>
     * The parallelism level defaults to {@code Runtime.availableProcessors() - 1}
     *
     * <br><br>
     * The collector maintains the order of processed {@link Stream}. Instances should not be reused.
     * <br><br>
     * On duplicate key, completes exceptionally with {@link IllegalStateException}
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Map<Integer, Integer>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToMap(i -> i, i -> i * 2, () -> new HashMap<>(), executor));
     * }</pre>
     *
     * @param keyMapper   the key deriving operation to be performed in parallel
     * @param valueMapper the value deriving operation to be performed in parallel
     * @param mapSupplier the factory returning a target {@code Map} instance
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param <T>         the type of the collected elements
     * @param <K>         the target {@code Map} key type
     * @param <V>         the target {@code Map} value type
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Map} in parallel
     *
     * @since 0.2.0
     */
    public static <T, K, V> Collector<T, ?, CompletableFuture<Map<K, V>>> parallelToMap(Function<T, K> keyMapper, Function<T, V> valueMapper, Supplier<Map<K, V>> mapSupplier, Executor executor) {
        return AsyncParallelCollector.collectingToMap(keyMapper, valueMapper, mapSupplier, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Map} of these elements
     *
     * <br><br>
     * Encounter order is preserved. Instances should not be reused.
     * On duplicate key, completes exceptionally with {@link IllegalStateException}
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Map<Integer, Integer>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToMap(i -> i, i -> i * 2, () -> new HashMap<>(), executor, 2));
     * }</pre>
     *
     * @param keyMapper   the key deriving operation to be performed in parallel
     * @param valueMapper the value deriving operation to be performed in parallel
     * @param mapSupplier the factory returning a target {@code Map} instance
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param parallelism the parallelism level
     * @param <T>         the type of the collected elements
     * @param <K>         the target {@code Map} key type
     * @param <V>         the target {@code Map} value type
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Map} in parallel
     *
     * @since 0.2.0
     */
    public static <T, K, V> Collector<T, ?, CompletableFuture<Map<K, V>>> parallelToMap(Function<T, K> keyMapper, Function<T, V> valueMapper, Supplier<Map<K, V>> mapSupplier, Executor executor, int parallelism) {
        return AsyncParallelCollector.collectingToMap(keyMapper, valueMapper, mapSupplier, executor, parallelism);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Map} of these elements
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
     * CompletableFuture<Map<Integer, Integer>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToMap(i -> i, i -> i * 2, HashMap::new, Integer::sum, executor));
     * }</pre>
     *
     * @param keyMapper   the key deriving operation to be performed in parallel
     * @param valueMapper the value deriving operation to be performed in parallel
     * @param mapSupplier the factory returning a target {@code Map} instance
     * @param merger      the duplicate key value resolution strategy
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param <T>         the type of the collected elements
     * @param <K>         the target {@code Map} key type
     * @param <V>         the target {@code Map} value type
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Map} in parallel
     *
     * @since 0.2.0
     */
    public static <T, K, V> Collector<T, ?, CompletableFuture<Map<K, V>>> parallelToMap(Function<T, K> keyMapper, Function<T, V> valueMapper, Supplier<Map<K, V>> mapSupplier, BinaryOperator<V> merger, Executor executor) {
        return AsyncParallelCollector.collectingToMap(keyMapper, valueMapper, mapSupplier, merger, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Map} of these elements
     *
     * <br><br>
     * Encounter order is preserved. Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Map<Integer, Integer>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToMap(i -> i, i -> i * 2, HashMap::new, Integer::sum, executor, 2));
     * }</pre>
     *
     * @param keyMapper   the key deriving operation to be performed in parallel
     * @param valueMapper the value deriving operation to be performed in parallel
     * @param mapSupplier the factory returning a target {@code Map} instance
     * @param merger      the duplicate key value resolution strategy
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param parallelism the parallelism level
     * @param <T>         the type of the collected elements
     * @param <K>         the target {@code Map} key type
     * @param <V>         the target {@code Map} value type
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Map} in parallel
     *
     * @since 0.2.0
     */
    public static <T, K, V> Collector<T, ?, CompletableFuture<Map<K, V>>> parallelToMap(Function<T, K> keyMapper, Function<T, V> valueMapper, Supplier<Map<K, V>> mapSupplier, BinaryOperator<V> merger, Executor executor, int parallelism) {
        return AsyncParallelCollector
          .collectingToMap(keyMapper, valueMapper, mapSupplier, merger, executor, parallelism);
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
     * @since 1.0.0
     * @deprecated use {@link ParallelCollectors#parallel(Function, Executor)} instead
     */
    @Deprecated // for removal
    public static <T, R> Collector<T, ?, Stream<R>> parallelMap(Function<T, R> mapper, Executor executor) {
        return ParallelCollectors.parallelToStream(mapper, executor);
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
     * @since 1.0.0
     * @deprecated use {@link ParallelCollectors#parallel(Function, Executor, int)} instead
     */
    @Deprecated // for removal
    public static <T, R> Collector<T, ?, Stream<R>> parallelMap(Function<T, R> mapper, Executor executor, int parallelism) {
        return ParallelCollectors.parallelToStream(mapper, executor, parallelism);
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
     * @since 1.0.0
     * @deprecated use {@link ParallelCollectors#parallelToOrderedStream(Function, Executor)} instead
     */
    @Deprecated // for removal
    public static <T, R> Collector<T, ?, Stream<R>> parallelMapOrdered(Function<T, R> mapper, Executor executor) {
        return ParallelCollectors.parallelToOrderedStream(mapper, executor);
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
     * @since 1.0.0
     * @deprecated use {@link ParallelCollectors#parallelToOrderedStream(Function, Executor, int)} instead
     */
    @Deprecated // for removal
    public static <T, R> Collector<T, ?, Stream<R>> parallelMapOrdered(Function<T, R> mapper, Executor executor, int parallelism) {
        return ParallelCollectors.parallelToOrderedStream(mapper, executor, parallelism);
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
