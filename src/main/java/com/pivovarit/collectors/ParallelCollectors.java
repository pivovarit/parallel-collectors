package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

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
     * @param <T>                the type of the input elements
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 0.0.1
     */
    public static <T, C extends Collection<T>> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<C>> parallelToCollection(Supplier<C> collectionSupplier, Executor executor) {
        requireNonNull(collectionSupplier, "collectionSupplier can't be null");
        requireNonNull(executor, "executor can't be null");
        return new AsyncParallelCollector<>(Supplier::get, collectionSupplier, executor);
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
     * @param <T>                the type of the input elements
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 0.0.1
     */
    public static <T, C extends Collection<T>> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<C>> parallelToCollection(Supplier<C> collectionSupplier, Executor executor, int parallelism) {
        requireNonNull(collectionSupplier, "collectionSupplier can't be null");
        requireNonNull(executor, "executor can't be null");
        assertParallelismValid(parallelism);
        return new AsyncParallelCollector<>(Supplier::get, collectionSupplier, executor, assertParallelismValid(parallelism));
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
     * @param <T>                the type of the input elements
     * @param <R>                the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 0.0.1
     */
    public static <T, R, C extends Collection<R>> Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>> parallelToCollection(Function<T, R> mapper, Supplier<C> collectionSupplier, Executor executor) {
        requireNonNull(collectionSupplier, "collectionSupplier can't be null");
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return new AsyncParallelCollector<>(mapper, collectionSupplier, executor);
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
     * @param <T>                the type of the input elements
     * @param <R>                the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Collection} in parallel
     *
     * @since 0.0.1
     */
    public static <T, R, C extends Collection<R>> Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>> parallelToCollection(Function<T, R> mapper, Supplier<C> collectionSupplier, Executor executor, int parallelism) {
        requireNonNull(collectionSupplier, "collectionSupplier can't be null");
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        assertParallelismValid(parallelism);
        return new AsyncParallelCollector<>(mapper, collectionSupplier, executor, parallelism);
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
     * @param <T>      the type of the input elements
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code List} in parallel
     *
     * @since 0.0.1
     */
    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<List<T>>> parallelToList(Executor executor) {
        requireNonNull(executor, "executor can't be null");
        return new AsyncParallelCollector<>(Supplier::get, ArrayList::new, executor);
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
     * @param <T>         the type of the input elements
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code List} in parallel
     *
     * @since 0.0.1
     */
    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<List<T>>> parallelToList(Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        assertParallelismValid(parallelism);
        return new AsyncParallelCollector<>(Supplier::get, ArrayList::new, executor, assertParallelismValid(parallelism));
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
     * @param <T>      the type of the input elements
     * @param <R>      the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code List} in parallel
     *
     * @since 0.0.1
     */
    public static <T, R> Collector<T, List<CompletableFuture<R>>, CompletableFuture<List<R>>> parallelToList(Function<T, R> mapper, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return new AsyncParallelCollector<>(mapper, ArrayList::new, executor);
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
     * @param <T>         the type of the input elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code List} in parallel
     *
     * @since 0.0.1
     */
    public static <T, R> Collector<T, List<CompletableFuture<R>>, CompletableFuture<List<R>>> parallelToList(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        assertParallelismValid(parallelism);
        return new AsyncParallelCollector<>(mapper, ArrayList::new, executor, assertParallelismValid(parallelism));
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
     * @param <T>      the type of the input elements
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Set} in parallel
     *
     * @since 0.0.1
     */
    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<Set<T>>> parallelToSet(Executor executor) {
        requireNonNull(executor, "executor can't be null");
        return new AsyncParallelCollector<>(Supplier::get, HashSet::new, executor);
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
     * @param <T>         the type of the input elements
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Set} in parallel
     *
     * @since 0.0.1
     */
    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<Set<T>>> parallelToSet(Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        assertParallelismValid(parallelism);
        return new AsyncParallelCollector<>(Supplier::get, HashSet::new, executor, assertParallelismValid(parallelism));
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
     * @param <T>      the type of the input elements
     * @param <R>      the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Set} in parallel
     *
     * @since 0.0.1
     */
    public static <T, R> Collector<T, List<CompletableFuture<R>>, CompletableFuture<Set<R>>> parallelToSet(Function<T, R> mapper, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return new AsyncParallelCollector<>(mapper, HashSet::new, executor);
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
     * @param <T>         the type of the input elements
     * @param <R>         the result returned by {@code mapper}
     *
     * @return a {@code Collector} which collects all input elements into a user-provided mutable {@code Set} in parallel
     *
     * @since 0.0.1
     */
    public static <T, R> Collector<T, List<CompletableFuture<R>>, CompletableFuture<Set<R>>> parallelToSet(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        assertParallelismValid(parallelism);
        return new AsyncParallelCollector<>(mapper, HashSet::new, executor, parallelism);
    }

    private static int assertParallelismValid(int parallelism) {
        if (parallelism < 1) throw new IllegalArgumentException("Parallelism can't be lower than 1");
        return parallelism;
    }
}
