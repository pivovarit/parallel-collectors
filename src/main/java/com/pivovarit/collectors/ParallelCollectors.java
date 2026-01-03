package com.pivovarit.collectors;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

/**
 * An umbrella class exposing static factory methods for instantiating parallel {@link Collector}s
 *
 * @author Grzegorz Piwowarek
 */
@NullMarked
public final class ParallelCollectors {

  private ParallelCollectors() {}

  /**
   * A convenience {@link Collector} that performs parallel computations using Virtual Threads and
   * returns a {@link CompletableFuture} containing the result of applying the user-provided {@link
   * Collector} to the mapped elements.
   *
   * <p>Each element is transformed using the provided {@code mapper} in parallel on Virtual
   * Threads, and the results are reduced according to the supplied {@code collector}. <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
   *   .collect(parallel(i -> foo(i), toList()));
   * }</pre>
   *
   * @param mapper transformation applied to each element
   * @param collector the {@code Collector} describing the reduction
   * @param <T> the input element type
   * @param <R> the type produced by {@code mapper}
   * @param <RR> the reduction result type produced by {@code collector}
   * @return a {@code Collector} producing a {@link CompletableFuture} of the reduced result
   * @since 3.0.0
   */
  public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper, Collector<R, ?, RR> collector) {

    Objects.requireNonNull(collector, "collector cannot be null");
    return Factory.collecting(s -> s.collect(collector), mapper);
  }

  /**
   * A convenience {@link Collector} that performs parallel computations by classifying input
   * elements using the provided {@code classifier}, applying the given {@code mapper}, and emitting
   * {@link Grouped} entries representing each batch.
   *
   * <p>The generated {@link Stream} of {@code Grouped<K, R>} instances is then reduced using the
   * user-provided {@code collector}, executed on Virtual Threads. Each group is processed
   * independently, and every batch is guaranteed to be processed on a single thread. The reduction
   * is applied to the grouped results rather than to the raw mapped elements. <br>
   * <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<List<Grouped<String, String>>> result = Stream.of(t1, t2, t3)
   *   .collect(parallelBy(Task::groupId, t -> compute(t), toList()));
   * }</pre>
   *
   * @param classifier function that groups elements into batches
   * @param mapper transformation applied to each element
   * @param collector the {@code Collector} describing the reduction for each batch
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the type produced by {@code mapper}
   * @param <RR> the reduction result type produced by {@code collector}
   * @return a {@code Collector} producing a {@link CompletableFuture} whose value is obtained by
   *     reducing the {@code Stream<Grouped<K, R>>} produced by the parallel classification
   * @since 3.4.0
   */
  public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Collector<Grouped<K, R>, ?, RR> collector) {

    Objects.requireNonNull(collector, "collector cannot be null");
    Objects.requireNonNull(classifier, "classifier cannot be null");

    return Factory.collectingBy(
        classifier, (Function<Stream<Grouped<K, R>>, RR>) s -> s.collect(collector), mapper);
  }

  /**
   * A convenience {@link Collector} that performs parallel computations using Virtual Threads and
   * returns a {@link CompletableFuture} containing the result of applying the user-provided {@link
   * Collector} to the mapped elements, with a configurable parallelism level.
   *
   * <p>Each element is transformed using the provided {@code mapper} in parallel on Virtual
   * Threads, and the results are reduced according to the supplied {@code collector}. <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
   *   .collect(parallel(i -> foo(i), toList(), 2));
   * }</pre>
   *
   * @param mapper transformation applied to each element
   * @param collector the {@code Collector} describing the reduction
   * @param parallelism the maximum degree of parallelism
   * @param <T> the input element type
   * @param <R> the type produced by {@code mapper}
   * @param <RR> the reduction result type produced by {@code collector}
   * @return a {@code Collector} producing a {@link CompletableFuture} of the reduced result
   * @since 3.2.0
   */
  public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper, Collector<R, ?, RR> collector, int parallelism) {

    Objects.requireNonNull(collector, "collector cannot be null");
    return Factory.collecting(s -> s.collect(collector), mapper, Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations by classifying input
   * elements using the provided {@code classifier}, applying the given {@code mapper}, and emitting
   * {@link Grouped} entries representing each batch.
   *
   * <p>The generated {@link Stream} of {@code Grouped<K, R>} instances is then reduced using the
   * user-provided {@code collector}, executed on Virtual Threads. Each group is processed
   * independently, and every batch is guaranteed to be processed on a single thread. The reduction
   * is applied to the grouped results rather than to the raw mapped elements. A configurable {@code
   * parallelism} parameter defines the maximum level of concurrent processing. <br>
   * <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<List<Grouped<String, String>>> result = Stream.of(t1, t2, t3)
   *   .collect(parallelBy(Task::groupId, t -> compute(t), toList(), 4));
   * }</pre>
   *
   * @param classifier function that groups elements into batches
   * @param mapper transformation applied to each element
   * @param collector reduction applied to the resulting {@code Stream<Grouped<K, R>>}
   * @param parallelism maximum allowed parallelism
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the type produced by {@code mapper}
   * @param <RR> the reduction result type produced by {@code collector}
   * @return a {@code Collector} producing a {@link CompletableFuture} whose value is obtained by
   *     reducing the {@code Stream<Grouped<K, R>>} produced by the parallel classification
   * @since 3.4.0
   */
  public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Collector<Grouped<K, R>, ?, RR> collector,
      int parallelism) {

    Objects.requireNonNull(collector, "collector cannot be null");

    return Factory.collectingBy(
        classifier,
        (Function<Stream<Grouped<K, R>>, RR>) s -> s.collect(collector),
        mapper,
        Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations on a custom {@link
   * Executor} and returns a {@link CompletableFuture} containing the result of applying the
   * user-provided {@link Collector} to the mapped elements, with a configurable parallelism level.
   *
   * <p>Each element is transformed using the provided {@code mapper} in parallel on the specified
   * {@code executor}, and the results are reduced according to the supplied {@code collector}. <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
   *   .collect(parallel(i -> foo(i), toList(), executor, 2));
   * }</pre>
   *
   * @param mapper transformation applied to each element
   * @param collector the {@code Collector} describing the reduction
   * @param executor the {@code Executor} used for asynchronous execution
   * @param parallelism the maximum degree of parallelism
   * @param <T> the input element type
   * @param <R> the type produced by {@code mapper}
   * @param <RR> the reduction result type produced by {@code collector}
   * @return a {@code Collector} producing a {@link CompletableFuture} of the reduced result
   * @since 2.0.0
   */
  public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper,
      Collector<R, ?, RR> collector,
      Executor executor,
      int parallelism) {

    Objects.requireNonNull(collector, "collector cannot be null");
    return Factory.collecting(
        s -> s.collect(collector),
        mapper,
        Options.executor(executor),
        Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations by classifying input
   * elements using the provided {@code classifier}, applying the given {@code mapper}, and emitting
   * {@link Grouped} entries representing each batch.
   *
   * <p>The generated {@link Stream} of {@code Grouped<K, R>} instances is then reduced using the
   * user-provided {@code collector}, executed on the supplied {@link Executor}. Each group is
   * processed independently, and every batch is guaranteed to be processed on a single thread. The
   * reduction is applied to grouped results rather than to the raw mapped elements. A configurable
   * {@code parallelism} parameter defines the maximum level of concurrent batch execution. <br>
   * <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<List<Grouped<String, String>>> result = Stream.of(t1, t2, t3)
   *   .collect(parallelBy(Task::groupId, t -> compute(t), toList(), executor, 4));
   * }</pre>
   *
   * @param classifier function that groups elements into batches
   * @param mapper transformation applied to each element
   * @param collector reduction applied to the resulting {@code Stream<Grouped<K, R>>}
   * @param executor the {@code Executor} used for asynchronous execution
   * @param parallelism maximum allowed parallelism
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the type produced by {@code mapper}
   * @param <RR> the reduction result type produced by {@code collector}
   * @return a {@code Collector} producing a {@link CompletableFuture} whose value is obtained by
   *     reducing the {@code Stream<Grouped<K, R>>} produced by the parallel classification
   * @since 3.4.0
   */
  public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Collector<Grouped<K, R>, ?, RR> collector,
      Executor executor,
      int parallelism) {

    Objects.requireNonNull(collector, "collector cannot be null");
    Objects.requireNonNull(classifier, "classifier cannot be null");
    return Factory.collectingBy(
        classifier,
        (Function<Stream<Grouped<K, R>>, RR>) s -> s.collect(collector),
        mapper,
        Options.executor(executor),
        Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations on a custom {@link
   * Executor} with effectively unlimited parallelism and returns a {@link CompletableFuture}
   * containing the result of applying the user-provided {@link Collector} to the mapped elements.
   *
   * <p>Each element is transformed using the provided {@code mapper} in parallel on the specified
   * {@code executor}, and the results are reduced according to the supplied {@code collector}. <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
   *   .collect(parallel(i -> foo(i), toList(), executor));
   * }</pre>
   *
   * @param mapper transformation applied to each element
   * @param collector the {@code Collector} describing the reduction
   * @param executor the {@code Executor} used for asynchronous execution
   * @param <T> the input element type
   * @param <R> the type produced by {@code mapper}
   * @param <RR> the reduction result type produced by {@code collector}
   * @return a {@code Collector} producing a {@link CompletableFuture} of the reduced result
   * @since 3.3.0
   */
  public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper, Collector<R, ?, RR> collector, Executor executor) {

    Objects.requireNonNull(collector, "collector cannot be null");
    return Factory.collecting(s -> s.collect(collector), mapper, Options.executor(executor));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations by classifying input
   * elements using the provided {@code classifier}, applying the given {@code mapper}, and emitting
   * {@link Grouped} entries representing each batch.
   *
   * <p>The generated {@link Stream} of {@code Grouped<K, R>} instances is then reduced using the
   * user-provided {@code collector}, executed on the supplied {@link Executor}. Each group is
   * processed independently, and every batch is guaranteed to be processed on a single thread. The
   * reduction is applied to grouped results rather than to the raw mapped elements. <br>
   * <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<List<Grouped<String, String>>> result = Stream.of(task1, task2, task3)
   *   .collect(parallelBy(
   *       Task::groupId,
   *       t -> compute(t),
   *       toList(),
   *       executor));
   * }</pre>
   *
   * @param classifier function that groups elements into batches
   * @param mapper transformation applied to each element
   * @param collector reduction applied to the resulting {@code Stream<Grouped<K, R>>}
   * @param executor the {@code Executor} used for asynchronous execution
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the type produced by {@code mapper}
   * @param <RR> the reduction result type produced by {@code collector}
   * @return a {@code Collector} producing a {@link CompletableFuture} whose value is obtained by
   *     reducing the {@code Stream<Grouped<K, R>>} produced by the parallel classification
   * @since 3.4.0
   */
  public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Collector<Grouped<K, R>, ?, RR> collector,
      Executor executor) {
    Objects.requireNonNull(collector, "collector cannot be null");
    return Factory.collectingBy(
        classifier,
        (Function<Stream<Grouped<K, R>>, RR>) s -> s.collect(collector),
        mapper,
        Options.executor(executor));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations using Virtual Threads and
   * returns a {@link CompletableFuture} containing a {@link Stream} of the mapped elements.
   *
   * <p>The collector maintains the encounter order of the processed elements. Instances of this
   * collector should not be reused. <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
   *   .collect(parallel(i -> foo()));
   * }</pre>
   *
   * @param mapper a transformation applied to each element
   * @param <T> the input element type
   * @param <R> the type produced by {@code mapper}
   * @return a {@code Collector} producing a {@link CompletableFuture} of a {@link Stream} of mapped
   *     elements
   * @since 3.0.0
   */
  public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper) {

    return Factory.collecting((Function<Stream<R>, Stream<R>>) i -> i, mapper);
  }

  /**
   * A convenience {@link Collector} that performs parallel computations by classifying input
   * elements using the provided {@code classifier}, applying the given {@code mapper}, and emitting
   * {@link Grouped} entries representing each batch.
   *
   * <p>The resulting {@link CompletableFuture} completes with a {@link Stream} of {@code Grouped<K,
   * R>} instances. Each group is processed independently on Virtual Threads, and every batch is
   * guaranteed to be processed on a single thread. The encounter order of elements within each
   * batch is preserved. <br>
   * <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<Stream<Grouped<String, String>>> result = Stream.of(t1, t2, t3)
   *   .collect(parallelBy(Task::groupId, t -> compute(t)));
   * }</pre>
   *
   * @param classifier function that groups elements into batches
   * @param mapper transformation applied to each element
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the type produced by {@code mapper}
   * @return a {@code Collector} producing a {@link CompletableFuture} whose value is a {@link
   *     Stream} of {@code Grouped<K, R>} produced by the parallel classification
   * @since 3.4.0
   */
  public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Grouped<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier, Function<? super T, ? extends R> mapper) {
    return Factory.collectingBy(classifier, mapper);
  }

  /**
   * A convenience {@link Collector} that performs parallel computations using Virtual Threads and
   * returns a {@link CompletableFuture} containing a {@link Stream} of the mapped elements.
   *
   * <p>The collector maintains the encounter order of the processed elements. Instances of this
   * collector should not be reused. <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
   *   .collect(parallel(i -> foo(), 4));
   * }</pre>
   *
   * @param mapper a transformation applied to each element
   * @param parallelism the maximum degree of parallelism
   * @param <T> the input element type
   * @param <R> the type produced by {@code mapper}
   * @return a {@code Collector} producing a {@link CompletableFuture} of a {@link Stream} of mapped
   *     elements
   * @since 3.2.0
   */
  public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper, int parallelism) {
    return Factory.collecting(
        (Function<Stream<R>, Stream<R>>) i -> i, mapper, Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations by classifying input
   * elements using the provided {@code classifier}, applying the given {@code mapper}, and emitting
   * {@link Grouped} entries representing each batch.
   *
   * <p>The resulting {@link CompletableFuture} completes with a {@link Stream} of {@code Grouped<K,
   * R>} instances. Each group is processed independently on Virtual Threads, and every batch is
   * guaranteed to be processed on a single thread. The encounter order of elements within each
   * batch is preserved. <br>
   * <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<Stream<Grouped<String, String>>> result = Stream.of(t1, t2, t3)
   *   .collect(parallelBy(Task::groupId, t -> compute(t), 4));
   * }</pre>
   *
   * @param classifier function that groups elements into batches
   * @param mapper transformation applied to each element
   * @param parallelism the maximum degree of parallelism
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the type produced by {@code mapper}
   * @return a {@code Collector} producing a {@link CompletableFuture} whose value is a {@link
   *     Stream} of {@code Grouped<K, R>} produced by the parallel classification
   * @since 3.4.0
   */
  public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Grouped<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      int parallelism) {
    return Factory.collectingBy(classifier, mapper, Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations on a custom {@link
   * Executor} and returns a {@link CompletableFuture} containing a {@link Stream} of the mapped
   * elements.
   *
   * <p>The collector maintains the encounter order of the processed elements. Instances of this
   * collector should not be reused. <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
   *   .collect(parallel(i -> foo(), executor, 2));
   * }</pre>
   *
   * @param mapper a transformation applied to each element
   * @param executor the {@code Executor} used for asynchronous execution
   * @param parallelism the maximum degree of parallelism
   * @param <T> the input element type
   * @param <R> the type produced by {@code mapper}
   * @return a {@code Collector} producing a {@link CompletableFuture} of a {@link Stream} of mapped
   *     elements
   * @since 2.0.0
   */
  public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper, Executor executor, int parallelism) {

    return Factory.collecting(
        (Function<Stream<R>, Stream<R>>) i -> i,
        mapper,
        Options.executor(executor),
        Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations by classifying elements
   * into groups using the provided {@code classifier}, executing each group asynchronously on the
   * supplied {@link Executor}, and returning a {@link CompletableFuture} that yields a {@link
   * Stream} of {@link Grouped} results.
   *
   * <p>Elements within each group preserve encounter order. Instances of this collector are not
   * intended to be reused. <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<Stream<Grouped<String, String>>> result = Stream.of(t1, t2, t3)
   *   .collect(parallelBy(Task::groupId, t -> compute(t), executor, 4));
   * }</pre>
   *
   * @param classifier function that assigns each input element to a group
   * @param mapper transformation applied to each element
   * @param executor the {@code Executor} used for asynchronous execution
   * @param parallelism the maximum degree of parallelism
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the mapped element type
   * @return a {@code Collector} producing a {@link CompletableFuture} of a {@link Stream} of {@link
   *     Grouped} results
   * @since 3.4.0
   */
  public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Grouped<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Executor executor,
      int parallelism) {
    return Factory.collectingBy(
        classifier, mapper, Options.executor(executor), Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations on a custom {@link
   * Executor} with effectively unlimited parallelism and returns a {@link CompletableFuture}
   * containing a {@link Stream} of the mapped elements.
   *
   * <p>The collector maintains the encounter order of the processed elements. Instances of this
   * collector should not be reused. <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
   *   .collect(parallel(i -> foo(), executor));
   * }</pre>
   *
   * @param mapper a transformation applied to each element
   * @param executor the {@code Executor} used for asynchronous execution
   * @param <T> the input element type
   * @param <R> the type produced by {@code mapper}
   * @return a {@code Collector} producing a {@link CompletableFuture} of a {@link Stream} of mapped
   *     elements
   * @since 3.3.0
   */
  public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper, Executor executor) {

    return Factory.collecting(
        (Function<Stream<R>, Stream<R>>) i -> i, mapper, Options.executor(executor));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations by classifying elements
   * using the provided {@code classifier}, executing each group asynchronously on the supplied
   * {@link Executor}, and returning a {@link CompletableFuture} that yields a {@link Stream} of
   * {@link Grouped} results.
   *
   * <p>Elements within a group preserve their encounter order. Instances of this collector are not
   * intended to be reused. <br>
   * Example:
   *
   * <pre>{@code
   * CompletableFuture<Stream<Grouped<String, String>>> future = Stream.of(t1, t2, t3)
   *   .collect(parallelBy(Task::groupId, t -> compute(t), executor));
   * }</pre>
   *
   * @param classifier function that assigns each element to a group
   * @param mapper transformation applied to each element
   * @param executor the {@code Executor} used for asynchronous execution
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the mapped element type
   * @return a {@code Collector} producing a {@link CompletableFuture} of a {@link Stream} of {@link
   *     Grouped} results
   * @since 3.4.0
   */
  public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Grouped<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Executor executor) {

    return Factory.collectingBy(classifier, mapper, Options.executor(executor));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations using Virtual Threads and
   * returns a {@link Stream} of the mapped elements as they arrive.
   *
   * <p>For a parallelism of 1, the stream is executed by the calling thread. <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(1, 2, 3)
   *   .collect(parallelToStream(i -> foo()))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param mapper a transformation applied to each element
   * @param <T> the input element type
   * @param <R> the type produced by {@code mapper}
   * @return a {@code Collector} producing a {@link Stream} of mapped elements in parallel
   * @since 3.0.0
   */
  public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper) {

    return Factory.streaming(mapper);
  }

  /**
   * A convenience {@link Collector} that performs parallel computations by classifying elements
   * using the provided {@code classifier}, applying the given {@code mapper}, and producing a
   * {@link Stream} of {@link Grouped} elements as they become available.
   *
   * <p>Each group is processed independently on Virtual Threads, and every batch is guaranteed to
   * be executed on a single thread. The encounter order within each group is preserved. <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(task1, task2, task3)
   *   .collect(parallelToStreamBy(Task::groupId, t -> compute(t)))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param classifier function that assigns each input element to a group
   * @param mapper transformation applied to each element
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the mapped element type
   * @return a {@code Collector} producing a {@link Stream} of {@link Grouped} elements computed in
   *     parallel
   * @since 3.4.0
   */
  public static <T, K, R> Collector<T, ?, Stream<Grouped<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier, Function<? super T, ? extends R> mapper) {

    return Factory.streamingBy(classifier, mapper);
  }

  /**
   * A convenience {@link Collector} that performs parallel computations using Virtual Threads and
   * returns a {@link Stream} of the mapped elements as they arrive.
   *
   * <p>For a parallelism of 1, the stream is executed by the calling thread. <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(1, 2, 3)
   *   .collect(parallelToStream(i -> foo(), 2))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param mapper a transformation applied to each element
   * @param parallelism the maximum degree of parallelism
   * @param <T> the input element type
   * @param <R> the type produced by {@code mapper}
   * @return a {@code Collector} producing a {@link Stream} of mapped elements in parallel
   * @since 3.2.0
   */
  public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper, int parallelism) {

    return Factory.streaming(mapper, Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations by classifying elements
   * using the provided {@code classifier}, applying the given {@code mapper}, and producing a
   * {@link Stream} of {@link Grouped} elements as they become available.
   *
   * <p>Each group is processed independently on Virtual Threads, and every batch is guaranteed to
   * be executed on a single thread. The encounter order within each group is preserved. A {@code
   * parallelism} parameter limits the maximum number of concurrently processed groups. <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(t1, t2, t3)
   *   .collect(parallelToStreamBy(Task::groupId, t -> compute(t), 2))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param classifier function that assigns each input element to a group
   * @param mapper transformation applied to each element
   * @param parallelism the maximum degree of parallelism
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the mapped element type
   * @return a {@code Collector} producing a {@link Stream} of {@link Grouped} elements computed in
   *     parallel
   * @since 3.4.0
   */
  public static <T, K, R> Collector<T, ?, Stream<Grouped<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      int parallelism) {

    return Factory.streamingBy(classifier, mapper, Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations on a custom {@link
   * Executor} with effectively unlimited parallelism and returns a {@link Stream} of the mapped
   * elements as they arrive.
   *
   * <p>For a parallelism of 1, the stream is executed by the calling thread. <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(1, 2, 3)
   *   .collect(parallelToStream(i -> foo(), executor))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param mapper a transformation applied to each element
   * @param executor the {@code Executor} used for asynchronous execution
   * @param <T> the input element type
   * @param <R> the type produced by {@code mapper}
   * @return a {@code Collector} producing a {@link Stream} of mapped elements in parallel
   * @since 3.3.0
   */
  public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper, Executor executor) {

    return Factory.streaming(mapper, Options.executor(executor));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations by classifying elements
   * using the provided {@code classifier}, applying the given {@code mapper}, and producing a
   * {@link Stream} of {@link Grouped} elements as they become available.
   *
   * <p>Each group is processed independently on the supplied {@link Executor}, and every batch is
   * guaranteed to be executed on a single thread. The encounter order within each group is
   * preserved. <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(t1, t2, t3)
   *   .collect(parallelToStreamBy(Task::groupId, t -> compute(t), executor))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param classifier function that assigns each input element to a group
   * @param mapper transformation applied to each element
   * @param executor the {@code Executor} used for asynchronous execution
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the mapped element type
   * @return a {@code Collector} producing a {@link Stream} of {@link Grouped} elements computed in
   *     parallel
   * @since 3.4.0
   */
  public static <T, K, R> Collector<T, ?, Stream<Grouped<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Executor executor) {

    return Factory.streamingBy(classifier, mapper, Options.executor(executor));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations on a custom {@link
   * Executor} and returns a {@link Stream} yielding results as soon as they are produced.
   *
   * <p>Each input element is submitted independently for asynchronous processing using the provided
   * {@code mapper}. Execution may proceed in parallel up to the configured {@code parallelism}.
   * When the parallelism is set to {@code 1}, all work is performed on the calling thread.
   *
   * <p>Ordering characteristics:
   *
   * <ul>
   *   <li>The encounter order of input elements is not preserved.
   *   <li>Results are emitted in the order in which they complete.
   * </ul>
   *
   * <p>Example:
   *
   * <pre>{@code
   * Stream.of(1, 2, 3)
   *   .collect(parallelToStream(i -> foo(i), executor, 2))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param mapper transformation applied to each element
   * @param executor the {@code Executor} used for asynchronous execution
   * @param parallelism the maximum degree of parallelism
   * @param <T> the input element type
   * @param <R> the result type produced by {@code mapper}
   * @return a {@code Collector} producing a {@link Stream} of mapped results as they complete
   * @since 2.0.0
   */
  public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper, Executor executor, int parallelism) {
    return Factory.streaming(mapper, Options.executor(executor), Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations on a custom {@link
   * Executor}, ensuring that all elements classified into the same group are processed on a single
   * thread.
   *
   * <p>Incoming elements are partitioned using the provided {@code classifier}. Each partition
   * (i.e., batch associated with a classification key) is executed as a unit, guaranteeing that all
   * elements within that batch are processed by the same thread. Different batches may run in
   * parallel depending on the configured {@code parallelism}.
   *
   * <p>Ordering guarantees:
   *
   * <ul>
   *   <li>Elements within the same batch preserve their encounter order.
   *   <li>Batches themselves may execute and complete in any order.
   * </ul>
   *
   * <p>The resulting {@link Stream} emits mapped results as soon as they are produced. If {@code
   * parallelism} is set to {@code 1}, all work is executed on the calling thread. <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(t1, t2, t3)
   *   .collect(parallelToStreamBy(Task::groupId, t -> compute(t), executor, 4))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param classifier function that assigns elements to groups; all elements with the same key are
   *     guaranteed to run on the same thread
   * @param mapper transformation applied to each element
   * @param executor the {@code Executor} used for asynchronous execution
   * @param parallelism the maximum allowed parallelism
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the mapped result type
   * @return a {@code Collector} producing a {@link Stream} of mapped results as they become
   *     available
   * @since 3.4.0
   */
  public static <T, K, R> Collector<T, ?, Stream<Grouped<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Executor executor,
      int parallelism) {
    return Factory.streamingBy(
        classifier, mapper, Options.executor(executor), Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations using Virtual Threads and
   * returns a {@link Stream} of the mapped elements as they arrive while maintaining the initial
   * order.
   *
   * <p>For a parallelism of 1, the stream is executed by the calling thread. <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(1, 2, 3)
   *   .collect(parallelToOrderedStream(i -> foo()))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param mapper a transformation applied to each element
   * @param <T> the input element type
   * @param <R> the type produced by {@code mapper}
   * @return a {@code Collector} producing a {@link Stream} of mapped elements in parallel,
   *     preserving order
   * @since 3.0.0
   */
  public static <T, R> Collector<T, ?, Stream<R>> parallelToOrderedStream(
      Function<? super T, ? extends R> mapper) {

    return Factory.streaming(mapper, Options.ordered());
  }

  /**
   * A convenience {@link Collector} that performs parallel computations by classifying elements
   * using the provided {@code classifier}, applying the given {@code mapper}, and producing a
   * {@link Stream} of {@link Grouped} elements as they become available while preserving the
   * original encounter order of input elements.
   *
   * <p>Ordering guarantees:
   *
   * <ul>
   *   <li>The encounter order of input elements is preserved within the resulting {@link Stream}.
   *   <li>Parallel execution does not affect the final ordering of elements in the resulting
   *       stream.
   * </ul>
   *
   * <p>Each group is processed independently on Virtual Threads, and every batch is guaranteed to
   * be executed on a single thread. <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(t1, t2, t3)
   *   .collect(parallelToOrderedStreamBy(Task::groupId, t -> compute(t)))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param classifier function that assigns elements to groups
   * @param mapper transformation applied to each element
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the mapped element type
   * @return a {@code Collector} producing a {@link Stream} of {@link Grouped} elements computed in
   *     parallel while preserving input order
   * @since 3.4.0
   */
  public static <T, K, R> Collector<T, ?, Stream<Grouped<K, R>>> parallelToOrderedStreamBy(
      Function<? super T, ? extends K> classifier, Function<? super T, ? extends R> mapper) {

    return Factory.streamingBy(classifier, mapper, Options.ordered());
  }

  /**
   * A convenience {@link Collector} that performs parallel computations using Virtual Threads while
   * preserving the encounter order of the input elements. Results are emitted in the same order as
   * the corresponding elements were encountered, regardless of when individual tasks complete.
   *
   * <p>Each element is submitted independently for asynchronous processing on Virtual Threads using
   * the provided {@code mapper}. Execution may proceed in parallel up to the configured {@code
   * parallelism}. When the parallelism is set to {@code 1}, all work is performed on the calling
   * thread.
   *
   * <p>Ordering guarantees:
   *
   * <ul>
   *   <li>The encounter order of input elements is preserved.
   *   <li>Parallel execution does not affect the final ordering of the resulting stream.
   * </ul>
   *
   * <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(1, 2, 3)
   *   .collect(parallelToOrderedStream(i -> foo(i), 2))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param mapper transformation applied to each element
   * @param parallelism the maximum degree of parallelism
   * @param <T> the input element type
   * @param <R> the mapped result type
   * @return a {@code Collector} producing an ordered {@link Stream} of mapped results
   * @since 3.2.0
   */
  public static <T, R> Collector<T, ?, Stream<R>> parallelToOrderedStream(
      Function<? super T, ? extends R> mapper, int parallelism) {
    return Factory.streaming(mapper, Options.ordered(), Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations using Virtual Threads while
   * preserving the encounter order of both batches and the elements within each batch.
   *
   * <p>Incoming elements are partitioned using the provided {@code classifier}. Each partition
   * (i.e., batch associated with a classification key) is processed as a unit. Although batches may
   * execute in parallel up to the configured {@code parallelism}, their results are emitted
   * strictly in encounter order.
   *
   * <p>Ordering guarantees:
   *
   * <ul>
   *   <li>Elements within a batch preserve their encounter order.
   *   <li>Batches are emitted in the encounter order of their first element.
   *   <li>Parallel execution does not affect the final ordering of the resulting stream.
   * </ul>
   *
   * <p>When {@code parallelism} is {@code 1}, all processing is performed on the calling thread.
   * <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(t1, t2, t3)
   *   .collect(parallelToOrderedStreamBy(Task::groupId, t -> compute(t), 4))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param classifier function that groups elements into batches
   * @param mapper transformation applied to each element
   * @param parallelism the maximum allowed parallelism
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the mapped result type
   * @return a {@code Collector} producing an ordered {@link Stream} of {@link Grouped} results
   * @since 3.4.0
   */
  public static <T, K, R> Collector<T, ?, Stream<Grouped<K, R>>> parallelToOrderedStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      int parallelism) {
    return Factory.streamingBy(
        classifier, mapper, Options.ordered(), Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations on a custom {@link
   * Executor} while preserving the encounter order of the input elements. Results are emitted in
   * the same order as the corresponding elements were encountered, regardless of when individual
   * tasks complete.
   *
   * <p>Each element is submitted independently for asynchronous processing using the provided
   * {@code mapper} and executed on the specified {@code executor}. Execution may proceed with
   * effectively unlimited parallelism. When all work is performed on the calling thread ({@code
   * parallelism} of 1), the stream behaves sequentially.
   *
   * <p>Ordering guarantees:
   *
   * <ul>
   *   <li>The encounter order of input elements is preserved.
   *   <li>Parallel execution does not affect the final ordering of the resulting stream.
   * </ul>
   *
   * <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(1, 2, 3)
   *   .collect(parallelToOrderedStream(i -> foo(i), executor))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param mapper transformation applied to each element
   * @param executor the {@code Executor} used for asynchronous execution
   * @param <T> the input element type
   * @param <R> the mapped result type
   * @return a {@code Collector} producing an ordered {@link Stream} of mapped results
   * @since 3.3.0
   */
  public static <T, R> Collector<T, ?, Stream<R>> parallelToOrderedStream(
      Function<? super T, ? extends R> mapper, Executor executor) {

    return Factory.streaming(mapper, Options.ordered(), Options.executor(executor));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations by classifying elements
   * into batches using the provided {@code classifier}, executed on a custom {@link Executor},
   * while preserving the encounter order of both batches and elements within each batch.
   *
   * <p>Each batch is processed as a unit on the specified executor. Results are emitted in the
   * encounter order of the batches and the elements within them, even though batches may execute
   * concurrently.
   *
   * <p>Ordering guarantees:
   *
   * <ul>
   *   <li>Elements within a batch preserve their encounter order.
   *   <li>Batches are emitted in the encounter order of their first element.
   * </ul>
   *
   * <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(t1, t2, t3)
   *   .collect(parallelToOrderedStreamBy(Task::groupId, t -> compute(t), executor))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param classifier function that assigns elements to groups; all elements with the same key are
   *     guaranteed to run on the same thread
   * @param mapper transformation applied to each element
   * @param executor the {@code Executor} used for asynchronous execution
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the mapped result type
   * @return a {@code Collector} producing an ordered {@link Stream} of {@link Grouped} results
   * @since 3.4.0
   */
  public static <T, K, R> Collector<T, ?, Stream<Grouped<K, R>>> parallelToOrderedStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Executor executor) {
    return Factory.streamingBy(classifier, mapper, Options.ordered(), Options.executor(executor));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations on a custom {@link
   * Executor} while preserving the encounter order of input elements. Results are emitted in the
   * same order as the corresponding elements were encountered, regardless of completion order.
   *
   * <p>Each element is submitted independently for asynchronous processing using the provided
   * {@code mapper} and executed on the specified {@code executor}. Execution may proceed in
   * parallel up to the configured {@code parallelism}. When {@code parallelism} is {@code 1}, all
   * work is executed on the calling thread.
   *
   * <p>Ordering guarantees:
   *
   * <ul>
   *   <li>The encounter order of input elements is preserved.
   *   <li>Parallel execution does not affect the final ordering of the resulting stream.
   * </ul>
   *
   * <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(1, 2, 3)
   *   .collect(parallelToOrderedStream(i -> foo(i), executor, 2))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param mapper transformation applied to each element
   * @param executor the {@code Executor} used for asynchronous execution
   * @param parallelism the maximum degree of parallelism
   * @param <T> the input element type
   * @param <R> the mapped result type
   * @return a {@code Collector} producing an ordered {@link Stream} of mapped results
   * @since 2.0.0
   */
  public static <T, R> Collector<T, ?, Stream<R>> parallelToOrderedStream(
      Function<? super T, ? extends R> mapper, Executor executor, int parallelism) {

    return Factory.streaming(
        mapper, Options.ordered(), Options.executor(executor), Options.parallelism(parallelism));
  }

  /**
   * A convenience {@link Collector} that performs parallel computations by classifying elements
   * into batches using the provided {@code classifier}, executed on a custom {@link Executor},
   * while preserving the encounter order of both batches and elements within each batch.
   *
   * <p>Each batch is processed as a unit on the specified executor. Results are emitted in the
   * encounter order of batches and of elements within each batch, even though batches may execute
   * concurrently up to the configured {@code parallelism}.
   *
   * <p>Ordering guarantees:
   *
   * <ul>
   *   <li>Elements within a batch preserve their encounter order.
   *   <li>Batches are emitted in the encounter order of their first element.
   * </ul>
   *
   * <br>
   * Example:
   *
   * <pre>{@code
   * Stream.of(t1, t2, t3)
   *   .collect(parallelToOrderedStreamBy(Task::groupId, t -> compute(t), executor, 4))
   *   .forEach(System.out::println);
   * }</pre>
   *
   * @param classifier function that assigns elements to groups; all elements with the same key are
   *     guaranteed to run on the same thread
   * @param mapper transformation applied to each element
   * @param executor the {@code Executor} used for asynchronous execution
   * @param parallelism the maximum allowed parallelism
   * @param <T> the input element type
   * @param <K> the classification key type
   * @param <R> the mapped result type
   * @return a {@code Collector} producing an ordered {@link Stream} of {@link Grouped} results
   * @since 3.4.0
   */
  public static <T, K, R> Collector<T, ?, Stream<Grouped<K, R>>> parallelToOrderedStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Executor executor,
      int parallelism) {

    return Factory.streamingBy(
        classifier,
        mapper,
        Options.ordered(),
        Options.executor(executor),
        Options.parallelism(parallelism));
  }

  /**
   * A convenience {@code Collector} for collecting a {@code Stream<CompletableFuture<T>>} into a
   * {@code CompletableFuture<R>} using a provided {@code Collector<T, ?, R>}
   *
   * @param collector the {@code Collector} describing the reduction
   * @param <T> the type of the collected elements
   * @param <R> the result of the transformation
   * @return a {@code Collector} which collects all futures and combines them into a single future
   *     using the provided downstream {@code Collector}
   * @since 2.3.0
   */
  public static <T, R> Collector<CompletableFuture<T>, ?, CompletableFuture<R>> toFuture(
      Collector<T, ?, R> collector) {
    return FutureCollectors.toFuture(collector);
  }

  /**
   * A convenience {@code Collector} for collecting a {@code Stream<CompletableFuture<T>>} into a
   * {@code CompletableFuture<List<T>>}
   *
   * @param <T> the type of the collected elements
   * @return a {@code Collector} which collects all futures and combines them into a single future
   *     returning a list of results
   * @since 2.3.0
   */
  public static <T> Collector<CompletableFuture<T>, ?, CompletableFuture<List<T>>> toFuture() {
    return FutureCollectors.toFuture();
  }

  /**
   * A subset of collectors which perform operations in batches and not separately (one object in a
   * thread pool's worker queue represents a batch of operations to be performed by a single thread)
   */
  public static final class Batching {

    private Batching() {}

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link
     * Executor} and returning them as a {@link CompletableFuture} containing a result of the
     * application of the user-provided {@link Collector}. <br>
     * Example:
     *
     * <pre>{@code
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo(i), toList(), executor, 2));
     * }</pre>
     *
     * @param mapper a transformation to be performed in parallel
     * @param collector the {@code Collector} describing the reduction
     * @param executor the {@code Executor} to use for asynchronous execution
     * @param parallelism the max parallelism level
     * @param <T> the type of the collected elements
     * @param <R> the result returned by {@code mapper}
     * @param <RR> the reduction result {@code collector}
     * @return a {@code Collector} which collects all processed elements into a user-provided
     *     mutable {@code Collection} in parallel
     * @since 2.1.0
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
        Function<? super T, ? extends R> mapper,
        Collector<R, ?, RR> collector,
        Executor executor,
        int parallelism) {
      Objects.requireNonNull(collector, "collector cannot be null");
      return Factory.collecting(
          s -> s.collect(collector),
          mapper,
          Options.batched(),
          Options.executor(executor),
          Options.parallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link
     * Executor} and returning them as {@link CompletableFuture} containing a {@link Stream} of
     * these elements. <br>
     * <br>
     * The collector maintains the order of processed {@link Stream}. Instances should not be
     * reused. <br>
     * Example:
     *
     * <pre>{@code
     * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo(), executor, 2));
     * }</pre>
     *
     * @param mapper a transformation to be performed in parallel
     * @param executor the {@code Executor} to use for asynchronous execution
     * @param parallelism the max parallelism level
     * @param <T> the type of the collected elements
     * @param <R> the result returned by {@code mapper}
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in
     *     parallel
     * @since 2.1.0
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
        Function<? super T, ? extends R> mapper, Executor executor, int parallelism) {
      return Factory.collecting(
          (Function<Stream<R>, Stream<R>>) i -> i,
          mapper,
          Options.batched(),
          Options.executor(executor),
          Options.parallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link
     * Executor} and returning a {@link Stream} instance returning results as they arrive.
     *
     * <p>For the parallelism of 1, the stream is executed by the calling thread. <br>
     * Example:
     *
     * <pre>{@code
     * Stream.of(1, 2, 3)
     *   .collect(parallelToStream(i -> foo(), executor, 2))
     *   .forEach(System.out::println);
     * }</pre>
     *
     * @param mapper a transformation to be performed in parallel
     * @param executor the {@code Executor} to use for asynchronous execution
     * @param parallelism the max parallelism level
     * @param <T> the type of the collected elements
     * @param <R> the result returned by {@code mapper}
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in
     *     parallel
     * @since 2.1.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
        Function<? super T, ? extends R> mapper, Executor executor, int parallelism) {
      return Factory.streaming(
          mapper, Options.batched(), Options.executor(executor), Options.parallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} used for executing parallel computations on a custom {@link
     * Executor} and returning a {@link Stream} instance returning results as they arrive while
     * maintaining the initial order.
     *
     * <p>For the parallelism of 1, the stream is executed by the calling thread. <br>
     * Example:
     *
     * <pre>{@code
     * Stream.of(1, 2, 3)
     *   .collect(parallelToOrderedStream(i -> foo(), executor, 2))
     *   .forEach(System.out::println);
     * }</pre>
     *
     * @param mapper a transformation to be performed in parallel
     * @param executor the {@code Executor} to use for asynchronous execution
     * @param parallelism the max parallelism level
     * @param <T> the type of the collected elements
     * @param <R> the result returned by {@code mapper}
     * @return a {@code Collector} which collects all processed elements into a {@code Stream} in
     *     parallel
     * @since 2.1.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToOrderedStream(
        Function<? super T, ? extends R> mapper, Executor executor, int parallelism) {
      return Factory.streaming(
          mapper,
          Options.ordered(),
          Options.batched(),
          Options.executor(executor),
          Options.parallelism(parallelism));
    }
  }
}
