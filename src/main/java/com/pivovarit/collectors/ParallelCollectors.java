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
     * <br><br>
     * Example:
     * <br><br>
     * <pre>Stream.of(1,2,3)
     * .map(i -> supplier(() -> blockingIO()))
     * .collect(inParallelToList(executor));
     * </pre>
     *
     * @param supplier a lambda expression to be converted into a type-safe {@code Supplier<T>} instance
     * @param <T>      value calculated by provided {@code Supplier<T>}
     * @return a type-safe {@code Supplier<T>} instance constructed from the supplier {@code Supplier<T>}
     * @since 0.0.1
     */
    public static <T> Supplier<T> supplier(Supplier<T> supplier) {
        return supplier;
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a user-provided {@link Collection} {@link R} of these elements
     * <br><br>
     * {@link Collector} is accepting {@link Supplier} instances so tasks need to be prepared beforehand and represented as {@link Supplier} implementations
     * <br><br>
     * Example:
     * <br><br>
     * <pre>CompletableFuture<TreeSet<String>> result = Stream.of(1, 2, 3)
     * .map(i -> supplier(() -> foo(i)))
     * .collect(inParallelToCollection(TreeSet::new, executor));
     * </pre>
     *
     * @param collection a custom {@link Supplier} providing a target {@link Collection} for computed values to be collected into
     * @param executor   a custom {@code Executor} which will be used to run parallel computations on
     * @since 0.0.1
     */
    public static <T, R extends Collection<T>> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<R>> inParallelToCollection(Supplier<R> collection, Executor executor) {
        return new ParallelCollector<>(Supplier::get, collection, executor);
    }

    /**
     * TODO
     * @param collection
     * @param executor
     * @param parallelism
     * @param <T>
     * @param <R>
     * @return
     */
    public static <T, R extends Collection<T>> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<R>> inParallelToCollection(Supplier<R> collection, Executor executor, int parallelism) {
        return new ThrottledParallelCollector<>(Supplier::get, collection, executor, assertParallelismValid(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a user-provided {@link Collection} {@link R} of these elements
     * <br><br>
     * Example:
     * <br><br>
     * <pre>CompletableFuture<TreeSet<String>> result = Stream.of(1, 2, 3)
     * .collect(inParallelToCollection(i -> foo(i), TreeSet::new, executor));
     * </pre>
     *
     * @param operation  a transformation to be performed in parallel
     * @param collection a custom {@link Supplier} providing a target {@link Collection} for computed values to be collected into
     * @param executor   a custom {@code Executor} which will be used to run parallel computations on
     * @since 0.0.1
     */
    public static <T, R, C extends Collection<R>> Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>> inParallelToCollection(Function<T, R> operation, Supplier<C> collection, Executor executor) {
        return new ParallelCollector<>(operation, collection, executor);
    }

    /**
     * TODO
     * @param operation
     * @param collection
     * @param executor
     * @param parallelism
     * @param <T>
     * @param <R>
     * @param <C>
     * @return
     */
    public static <T, R, C extends Collection<R>> Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>> inParallelToCollection(Function<T, R> operation, Supplier<C> collection, Executor executor, int parallelism) {
        return new ParallelCollector<>(operation, collection, executor);
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link List} of these elements
     * <br><br>
     * Example:
     * <br><br>
     * <pre>CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     * .map(i -> supplier(() -> foo(i)))
     * .collect(inParallelToList(executor));
     * </pre>
     *
     * @param executor a custom {@code Executor} which will be used to run parallel computations on
     * @since 0.0.1
     */
    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<List<T>>> inParallelToList(Executor executor) {
        return new ParallelCollector<>(Supplier::get, ArrayList::new, executor);
    }

    /**
     * TODO
     * @param executor
     * @param parallelism
     * @param <T>
     * @return
     */
    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<List<T>>> inParallelToList(Executor executor, int parallelism) {
        return new ThrottledParallelCollector<>(Supplier::get, ArrayList::new, executor, assertParallelismValid(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link List} of these elements
     *
     * <br><br>
     * Example:
     * <br><br>
     * <pre>CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     * .collect(inParallelToList(i -> foo(), executor));
     * </pre>
     *
     * @param operation a transformation to be performed in parallel
     * @param executor  a custom {@code Executor} which will be used to run parallel computations on
     * @since 0.0.1
     */
    public static <T, R> Collector<T, List<CompletableFuture<R>>, CompletableFuture<List<R>>> inParallelToList(Function<T, R> operation, Executor executor) {
        return new ParallelCollector<>(operation, ArrayList::new, executor);
    }

    /**
     * TODO
     * @param operation
     * @param executor
     * @param parallelism
     * @param <T>
     * @param <R>
     * @return
     */
    public static <T, R> Collector<T, List<CompletableFuture<R>>, CompletableFuture<List<R>>> inParallelToList(Function<T, R> operation, Executor executor, int parallelism) {
        return new ThrottledParallelCollector<>(operation, ArrayList::new, executor, assertParallelismValid(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing an {@link HashSet} of these elements
     * <br><br>
     * Example:
     * <br><br>
     * <pre>CompletableFuture<Set<String>> result = Stream.of(1, 2, 3)
     * .map(i -> supplier(() -> foo(i)))
     * .collect(inParallelToSet(executor));
     * </pre>
     *
     * @param executor a custom {@code Executor} which will be used to run parallel computations on
     * @since 0.0.1
     */
    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<Set<T>>> inParallelToSet(Executor executor) {
        return new ParallelCollector<>(Supplier::get, HashSet::new, executor);
    }

    /**
     * TODO
     * @param executor
     * @param parallelism
     * @param <T>
     * @return
     */
    public static <T> Collector<Supplier<T>, List<CompletableFuture<T>>, CompletableFuture<Set<T>>> inParallelToSet(Executor executor, int parallelism) {
        return new ThrottledParallelCollector<>(Supplier::get, HashSet::new, executor, assertParallelismValid(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link Executor}
     * and returning them as {@link CompletableFuture} containing a {@link Set} of these elements
     *
     * <br><br>
     * Example:
     * <br><br>
     * <pre>CompletableFuture<Set<String>> result = Stream.of(1, 2, 3)
     * .collect(inParallelToSet(i -> foo(), executor));
     * </pre>
     *
     * @param operation a transformation to be performed in parallel
     * @param executor  a custom {@code Executor} which will be used to run parallel computations on
     * @since 0.0.1
     */
    public static <T, R> Collector<T, List<CompletableFuture<R>>, CompletableFuture<Set<R>>> inParallelToSet(Function<T, R> operation, Executor executor) {
        return new ParallelCollector<>(operation, HashSet::new, executor);
    }

    /**
     * TODO
     * @param operation
     * @param executor
     * @param parallelism
     * @param <T>
     * @param <R>
     * @return
     */
    public static <T, R> Collector<T, List<CompletableFuture<R>>, CompletableFuture<Set<R>>> inParallelToSet(Function<T, R> operation, Executor executor, int parallelism) {
        return new ThrottledParallelCollector<>(operation, HashSet::new, executor, parallelism);
    }

    private static int assertParallelismValid(int parallelism) {
        if (parallelism < 1) throw new IllegalArgumentException("Parallelism can't be lower than 1");
        return parallelism;
    }
}
