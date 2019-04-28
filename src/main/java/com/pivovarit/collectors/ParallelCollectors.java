package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * An umbrella class exposing static factory methods for instantiating parallel {@link Collector}s
 *
 * @author Grzegorz Piwowarek
 */
public final class ParallelCollectors {

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(100,
          r -> {
              Thread thread = new Thread(r);
              thread.setDaemon(true);
              return thread;
          });
        Stream.iterate(0, i -> i + 1)
          .limit(100)
          .collect(parallelMap(withRandomDelay(), executorService, 100))
          .forEach(System.out::println);
    }

    private static Function<Integer, Integer> withRandomDelay() {
        return i -> {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(10000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return i;
        };
    }

    private ParallelCollectors() {
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as a {@link CompletableFuture} containing a user-provided {@link Collection}<{@link R}> of these elements.
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
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 0.0.1
     */
    public static <T, R, C extends Collection<R>> Collector<T, ?, CompletableFuture<C>> parallelToCollection(Function<T, R> mapper, Supplier<C> collectionSupplier, Executor executor) {
        requireNonNull(collectionSupplier, "collectionSupplier can't be null");
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return new AsyncParallelCollectionCollector<>(mapper, collectionSupplier, executor);
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
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 0.0.1
     */
    public static <T, R, C extends Collection<R>> Collector<T, ?, CompletableFuture<C>> parallelToCollection(Function<T, R> mapper, Supplier<C> collectionSupplier, Executor executor, int parallelism) {
        requireNonNull(collectionSupplier, "collectionSupplier can't be null");
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        assertParallelismValid(parallelism);
        return new AsyncParallelCollectionCollector<>(mapper, collectionSupplier, executor, parallelism);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link List} of these elements
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
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToList(i -> foo(), executor));
     * }</pre>
     *
     * @param mapper   a transformation to be performed in parallel
     * @param executor the {@code Executor} to use for asynchronous execution
     * @param <T>      the type of the collected elements
     * @param <R>      the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code List} in parallel
     *
     * @since 0.0.1
     */
    public static <T, R> Collector<T, ?, CompletableFuture<List<R>>> parallelToList(Function<T, R> mapper, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return new AsyncParallelCollectionCollector<>(mapper, defaultListImpl(), executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link List} of these elements
     *
     * <br><br>
     * Encounter order is preserved. Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToList(i -> foo(), executor, 2));
     * }</pre>
     *
     * @param mapper      a transformation to be performed in parallel
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param parallelism the parallelism level
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code List} in parallel
     *
     * @since 0.0.1
     */
    public static <T, R> Collector<T, ?, CompletableFuture<List<R>>> parallelToList(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        assertParallelismValid(parallelism);
        return new AsyncParallelCollectionCollector<>(mapper, defaultListImpl(), executor, assertParallelismValid(parallelism));
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
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Set<R>>> parallelToSet(Function<T, R> mapper, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return new AsyncParallelCollectionCollector<>(mapper, HashSet::new, executor);
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
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Set<R>>> parallelToSet(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        assertParallelismValid(parallelism);
        return new AsyncParallelCollectionCollector<>(mapper, HashSet::new, executor, parallelism);
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
        requireNonNull(executor, "executor can't be null");
        requireNonNull(keyMapper, "keyMapper can't be null");
        requireNonNull(valueMapper, "valueMapper can't be null");
        return new AsyncParallelMapCollector<>(keyMapper, valueMapper, uniqueKeyMerger(), defaultMapImpl(), executor);
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
        requireNonNull(executor, "executor can't be null");
        requireNonNull(keyMapper, "keyMapper can't be null");
        requireNonNull(valueMapper, "valueMapper can't be null");
        assertParallelismValid(parallelism);
        return new AsyncParallelMapCollector<>(keyMapper, valueMapper, uniqueKeyMerger(), defaultMapImpl(), executor, parallelism);
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
        requireNonNull(executor, "executor can't be null");
        requireNonNull(keyMapper, "keyMapper can't be null");
        requireNonNull(valueMapper, "valueMapper can't be null");
        requireNonNull(merger, "merger can't be null");
        return new AsyncParallelMapCollector<>(keyMapper, valueMapper, merger, defaultMapImpl(), executor);
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
        requireNonNull(executor, "executor can't be null");
        requireNonNull(keyMapper, "keyMapper can't be null");
        requireNonNull(valueMapper, "valueMapper can't be null");
        requireNonNull(merger, "merger can't be null");
        assertParallelismValid(parallelism);
        return new AsyncParallelMapCollector<>(keyMapper, valueMapper, merger, defaultMapImpl(), executor, parallelism);
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
        requireNonNull(executor, "executor can't be null");
        requireNonNull(keyMapper, "keyMapper can't be null");
        requireNonNull(valueMapper, "valueMapper can't be null");
        requireNonNull(mapSupplier, "mapSupplier can't be null");
        return new AsyncParallelMapCollector<>(keyMapper, valueMapper, uniqueKeyMerger(), mapSupplier, executor);
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
        requireNonNull(executor, "executor can't be null");
        requireNonNull(keyMapper, "keyMapper can't be null");
        requireNonNull(valueMapper, "valueMapper can't be null");
        requireNonNull(mapSupplier, "mapSupplier can't be null");
        assertParallelismValid(parallelism);
        return new AsyncParallelMapCollector<>(keyMapper, valueMapper, uniqueKeyMerger(), mapSupplier, executor, parallelism);
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
        requireNonNull(executor, "executor can't be null");
        requireNonNull(keyMapper, "keyMapper can't be null");
        requireNonNull(valueMapper, "valueMapper can't be null");
        requireNonNull(merger, "merger can't be null");
        requireNonNull(mapSupplier, "mapSupplier can't be null");
        return new AsyncParallelMapCollector<>(keyMapper, valueMapper, merger, mapSupplier, executor);
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
        requireNonNull(executor, "executor can't be null");
        requireNonNull(keyMapper, "keyMapper can't be null");
        requireNonNull(valueMapper, "valueMapper can't be null");
        requireNonNull(merger, "merger can't be null");
        requireNonNull(mapSupplier, "mapSupplier can't be null");
        assertParallelismValid(parallelism);
        return new AsyncParallelMapCollector<>(keyMapper, valueMapper, merger, mapSupplier, executor, parallelism);
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
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToList(i -> foo(), executor));
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
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallelToStream(Function<T, R> mapper, Executor executor) {
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
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToList(i -> foo(), executor, 2));
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
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallelToStream(Function<T, R> mapper, Executor executor, int parallelism) {
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
     * @param mapper      a transformation to be performed in parallel
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 0.4.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelMap(Function<T, R> mapper, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return new SyncCompletionOrderParallelCollector<>(mapper, executor);
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
     * @since 0.4.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelMap(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        assertParallelismValid(parallelism);
        return new SyncCompletionOrderParallelCollector<>(mapper, executor, parallelism);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning a {@link Stream} instance returning results as they arrive while maintaining the initial order.
     *
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
     * @param mapper      a transformation to be performed in parallel
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param <T>         the type of the collected elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in parallel
     *
     * @since 0.4.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelMapOrdered(Function<T, R> mapper, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return new SyncOrderedStreamParallelCollector<>(mapper, executor);
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
     * @since 0.4.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelMapOrdered(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        assertParallelismValid(parallelism);
        return new SyncOrderedStreamParallelCollector<>(mapper, executor, parallelism);
    }

    private static int assertParallelismValid(int parallelism) {
        if (parallelism < 1) throw new IllegalArgumentException("Parallelism can't be lower than 1");
        return parallelism;
    }

    private static <V> BinaryOperator<V> uniqueKeyMerger() {
        return (i1, i2) -> { throw new IllegalStateException("Duplicate key found"); };
    }

    private static <K, V> Supplier<Map<K, V>> defaultMapImpl() {
        return HashMap::new;
    }

    private static <R> Supplier<List<R>> defaultListImpl() {
        return ArrayList::new;
    }
}
