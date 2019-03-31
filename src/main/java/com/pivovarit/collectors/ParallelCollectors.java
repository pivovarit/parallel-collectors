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

    private ParallelCollectors() {
    }

    /**
     * A convenience method for constructing Lambda Expression-based {@link Supplier} instances from another Lambda Expression
     * to be used in conjuction with other static factory methods found in {@link ParallelCollectors}
     *
     * <br>
     * Example:
     * <pre>{@code
     * Stream.of(1,2,3)
     *   .map(i -> supplier(() -> blockingIO()))
     *   .collect(parallelToList(executor));
     * }</pre>
     *
     * @param supplier a lambda expression to be converted into a type-safe {@code Supplier<T>} instance
     * @param <T>      value calculated by provided {@code Supplier<T>}
     *
     * @return a type-safe {@code Supplier<T>} instance constructed from the supplier {@code Supplier<T>}
     *
     * @since 0.0.1
     */
    @Deprecated() // for removal in 1.0.0
    public static <T> Supplier<T> supplier(Supplier<T> supplier) {
        requireNonNull(supplier);
        return supplier;
    }

    /**
     * A convenience {@link Collector} for executing parallel computations on a custom {@link Executor} instance
     * and returning them as {@link CompletableFuture} containing a user-provided {@link Collection} {@link C} of these elements.
     *
     * <br><br>
     * No ordering guarantees provided. Instances should not be reused.
     *
     * <br><br>
     * Warning: this implementation can't be used with infinite {@link java.util.stream.Stream} instances.
     * It will try to submit {@code N} tasks to a provided {@link Executor}
     * where {@code N} is a number of elements in a {@link java.util.stream.Stream} instance
     *
     * <br><br>
     * {@link Collector} is accepting {@link Supplier} instances so tasks need to be prepared beforehand
     * and represented as {@link Supplier} implementations
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<TreeSet<String>> result = Stream.of(1, 2, 3)
     *   .map(i -> supplier(() -> foo(i)))
     *   .collect(parallelToCollection(TreeSet::new, executor));
     * }</pre>
     *
     * @param collectionSupplier a {@code Supplier} which returns a mutable {@code Collection} of the appropriate type
     * @param executor           the {@code Executor} to use for asynchronous execution
     * @param <T>                the type of the collected elements
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 0.0.1
     */
    @Deprecated() // for removal in 1.0.0
    public static <T, C extends Collection<T>> Collector<Supplier<T>, ?, CompletableFuture<C>> parallelToCollection(Supplier<C> collectionSupplier, Executor executor) {
        requireNonNull(collectionSupplier, "collectionSupplier can't be null");
        requireNonNull(executor, "executor can't be null");
        return new AsyncUnorderedParallelCollector<>(Supplier::get, collectionSupplier, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a user-provided {@link Collection} {@link C} of these elements.
     *
     * <br><br>
     * No ordering guarantees provided. Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<TreeSet<String>> result = Stream.of(1, 2, 3)
     *   .map(i -> supplier(() -> foo(i)))
     *   .collect(
     *     parallelToCollection(TreeSet::new, executor, 2));
     * }</pre>
     *
     * @param collectionSupplier a {@code Supplier} which returns a mutable {@code Collection} of the appropriate type
     * @param executor           the {@code Executor} to use for asynchronous execution
     * @param parallelism        the parallelism level
     * @param <T>                the type of the collected elements
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 0.0.1
     */
    @Deprecated() // for removal in 1.0.0
    public static <T, C extends Collection<T>> Collector<Supplier<T>, ?, CompletableFuture<C>> parallelToCollection(Supplier<C> collectionSupplier, Executor executor, int parallelism) {
        requireNonNull(collectionSupplier, "collectionSupplier can't be null");
        requireNonNull(executor, "executor can't be null");
        assertParallelismValid(parallelism);
        return new AsyncUnorderedParallelCollector<>(Supplier::get, collectionSupplier, executor, assertParallelismValid(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a user-provided {@link Collection} {@link R} of these elements
     *
     * <br><br>
     * No ordering guarantees provided. Instances should not be reused.
     *
     * <br><br>
     * Warning: this implementation can't be used with infinite {@link java.util.stream.Stream} instances.
     * It will try to submit {@code N} tasks to a provided {@link Executor}
     * where {@code N} is a number of elements in a {@link java.util.stream.Stream} instance
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
        return new AsyncUnorderedParallelCollector<>(mapper, collectionSupplier, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a user-provided {@link Collection} {@link R} of these elements
     *
     * <br><br>
     * No ordering guarantees provided. Instances should not be reused.
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
        return new AsyncUnorderedParallelCollector<>(mapper, collectionSupplier, executor, parallelism);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link List} of these elements
     *
     * <br><br>
     * No ordering guarantees provided. Instances should not be reused.
     *
     * <br><br>
     * Warning: this implementation can't be used with infinite {@link java.util.stream.Stream} instances.
     * It will try to submit {@code N} tasks to a provided {@link Executor}
     * where {@code N} is a size of a collected {@link java.util.stream.Stream}
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .map(i -> supplier(() -> foo(i)))
     *   .collect(parallelToList(executor));
     * }</pre>
     *
     * @param executor the {@code Executor} to use for asynchronous execution
     * @param <T>      the type of the collected elements
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code List} in parallel
     *
     * @since 0.0.1
     */
    @Deprecated() // for removal in 1.0.0
    public static <T> Collector<Supplier<T>, ?, CompletableFuture<List<T>>> parallelToList(Executor executor) {
        requireNonNull(executor, "executor can't be null");
        return new AsyncUnorderedParallelCollector<>(Supplier::get, defaultListImpl(), executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link List} of these elements
     *
     * <br><br>
     * No ordering guarantees provided. Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>
     * {@code
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .map(i -> supplier(() -> foo(i)))
     *   .collect(parallelToList(executor, 2));
     * }
     * </pre>
     *
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param parallelism the parallelism level
     * @param <T>         the type of the collected elements
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code List} in parallel
     *
     * @since 0.0.1
     */
    @Deprecated() // for removal in 1.0.0
    public static <T> Collector<Supplier<T>, ?, CompletableFuture<List<T>>> parallelToList(Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        assertParallelismValid(parallelism);
        return new AsyncUnorderedParallelCollector<>(Supplier::get, defaultListImpl(), executor, assertParallelismValid(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link List} of these elements
     *
     * <br><br>
     * No ordering guarantees provided. Instances should not be reused.
     *
     * <br><br>
     * Warning: this implementation can't be used with infinite {@link java.util.stream.Stream} instances.
     * It will try to submit {@code N} tasks to a provided {@link Executor}
     * where {@code N} is a size of a collected {@link java.util.stream.Stream}
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
        return new AsyncUnorderedParallelCollector<>(mapper, defaultListImpl(), executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link List} of these elements
     *
     * <br><br>
     * No ordering guarantees provided. Instances should not be reused.
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
        return new AsyncUnorderedParallelCollector<>(mapper, defaultListImpl(), executor, assertParallelismValid(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing an ordered {@link List} of these elements
     *
     * <br><br>
     * Original ordering preserved. Instances should not be reused.
     *
     * <br><br>
     * Warning: this implementation can't be used with infinite {@link java.util.stream.Stream} instances.
     * It will try to submit {@code N} tasks to a provided {@link Executor}
     * where {@code N} is a size of a collected {@link java.util.stream.Stream}
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
     * @since 0.1.0
     */
    public static <T, R> Collector<T, ?, CompletableFuture<List<R>>> parallelToListOrdered(Function<T, R> mapper, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return new AsyncOrderedParallelCollector<>(mapper, defaultListImpl(), executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing an ordered {@link List} of these elements
     *
     * <br><br>
     * Original ordering preserved. Instances should not be reused.
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
     * @since 0.1.0
     */
    public static <T, R> Collector<T, ?, CompletableFuture<List<R>>> parallelToListOrdered(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        assertParallelismValid(parallelism);
        return new AsyncOrderedParallelCollector<>(mapper, defaultListImpl(), executor, assertParallelismValid(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing an ordered {@link List} of these elements
     *
     * <br><br>
     * Original ordering preserved. Instances should not be reused.
     *
     * <br><br>
     * Warning: this implementation can't be used with infinite {@link java.util.stream.Stream} instances.
     * It will try to submit {@code N} tasks to a provided {@link Executor}
     * where {@code N} is a size of a collected {@link java.util.stream.Stream}
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToList(i -> foo(), executor));
     * }</pre>
     *
     * @param mapper       a transformation to be performed in parallel
     * @param listSupplier a {@code Supplier} which returns a mutable {@code List} of the appropriate type
     * @param executor     the {@code Executor} to use for asynchronous execution
     * @param <T>          the type of the collected elements
     * @param <R>          the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code List} in parallel
     *
     * @since 0.2.0
     */
    public static <T, R, C extends List<R>> Collector<T, ?, CompletableFuture<C>> parallelToListOrdered(Function<T, R> mapper, Supplier<C> listSupplier, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireNonNull(listSupplier, "listSupplier can't be null");
        return new AsyncOrderedParallelCollector<>(mapper, listSupplier, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing an ordered {@link List} of these elements
     *
     * <br><br>
     * Original ordering preserved. Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallelToList(i -> foo(), executor, 2));
     * }</pre>
     *
     * @param mapper       a transformation to be performed in parallel
     * @param listSupplier a {@code Supplier} which returns a target {@code List} of the provided type
     * @param executor     the {@code Executor} to use for asynchronous execution
     * @param parallelism  the parallelism level
     * @param <T>          the type of the collected elements
     * @param <R>          the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code List} in parallel
     *
     * @since 0.2.0
     */
    public static <T, R, C extends List<R>> Collector<T, ?, CompletableFuture<C>> parallelToListOrdered(Function<T, R> mapper, Supplier<C> listSupplier, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireNonNull(listSupplier, "listSupplier can't be null");
        assertParallelismValid(parallelism);
        return new AsyncOrderedParallelCollector<>(mapper, listSupplier, executor, assertParallelismValid(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing an {@link HashSet} of these element
     *
     * <br><br>
     * No ordering guarantees provided. Instances should not be reused.
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
     *   .map(i -> supplier(() -> foo(i)))
     *   .collect(parallelToSet(executor));
     * }</pre>
     *
     * @param executor the {@code Executor} to use for asynchronous execution
     * @param <T>      the type of the collected elements
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Set} in parallel
     *
     * @since 0.0.1
     */
    @Deprecated() // for removal in 1.0.0
    public static <T> Collector<Supplier<T>, ?, CompletableFuture<Set<T>>> parallelToSet(Executor executor) {
        requireNonNull(executor, "executor can't be null");
        return new AsyncUnorderedParallelCollector<>(Supplier::get, HashSet::new, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing an {@link HashSet} of these elements
     *
     * <br><br>
     * No ordering guarantees provided. Instances should not be reused.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Set<String>> result = Stream.of(1, 2, 3)
     *   .map(i -> supplier(() -> foo(i)))
     *   .collect(parallelToSet(executor, 2));
     * }</pre>
     *
     * @param executor    the {@code Executor} to use for asynchronous execution
     * @param parallelism the parallelism level
     * @param <T>         the type of the collected elements
     *
     * @return a {@code Collector} which collects all processed elements into a user-provided mutable {@code Set} in parallel
     *
     * @since 0.0.1
     */
    @Deprecated() // for removal in 1.0.0
    public static <T> Collector<Supplier<T>, ?, CompletableFuture<Set<T>>> parallelToSet(Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        assertParallelismValid(parallelism);
        return new AsyncUnorderedParallelCollector<>(Supplier::get, HashSet::new, executor, assertParallelismValid(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Set} of these elements
     *
     * <br><br>
     * No ordering guarantees provided. Instances should not be reused.
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
        return new AsyncUnorderedParallelCollector<>(mapper, HashSet::new, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Set} of these elements
     *
     * <br><br>
     * No ordering guarantees provided. Instances should not be reused.
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
        return new AsyncUnorderedParallelCollector<>(mapper, HashSet::new, executor, parallelism);
    }


    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Map} of these elements
     *
     * <br><br>
     * No ordering guarantees provided. Instances should not be reused.
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
     * No ordering guarantees provided. Instances should not be reused.
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
     * No ordering guarantees provided. Instances should not be reused.
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
     * No ordering guarantees provided. Instances should not be reused.
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
     * No ordering guarantees provided. Instances should not be reused.
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
     * No ordering guarantees provided. Instances should not be reused.
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
     * No ordering guarantees provided. Instances should not be reused.
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
     * No ordering guarantees provided. Instances should not be reused.
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
     * No ordering guarantees provided. Instances should not be reused.
     *
     * <br><br>
     * Warning: this implementation can't be used with infinite {@link java.util.stream.Stream} instances.
     * It will try to submit {@code N} tasks to a provided {@link Executor}
     * where {@code N} is a size of a collected {@link java.util.stream.Stream}
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
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return new AsyncUnorderedParallelStreamCollector<>(mapper, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Stream} of these elements
     *
     * <br><br>
     * No ordering guarantees provided. Instances should not be reused.
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
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        assertParallelismValid(parallelism);
        return new AsyncUnorderedParallelStreamCollector<>(mapper, executor, assertParallelismValid(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Stream} of these elements with encounter order preserved
     *
     * <br><br>
     * Warning: this implementation can't be used with infinite {@link java.util.stream.Stream} instances.
     * It will try to submit {@code N} tasks to a provided {@link Executor}
     * where {@code N} is a size of a collected {@link java.util.stream.Stream}
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
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallelToStreamOrdered(Function<T, R> mapper, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return new AsyncOrderedParallelStreamCollector<>(mapper, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Stream} of these elements with encounter order preserved
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
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallelToStreamOrdered(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        assertParallelismValid(parallelism);
        return new AsyncOrderedParallelStreamCollector<>(mapper, executor, assertParallelismValid(parallelism));
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
