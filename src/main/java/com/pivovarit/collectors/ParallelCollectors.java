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
package com.pivovarit.collectors;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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

    private ParallelCollectors() {
    }

    // defaults

    /**
     * A convenience {@link Collector} that performs parallel computations using Virtual Threads
     * and returns a {@link CompletableFuture} containing the result of applying the user-provided
     * {@link Collector} to the mapped elements.
     * <p>
     * Each element is transformed using the provided {@code mapper} in parallel on Virtual Threads,
     * and the results are reduced according to the supplied {@code collector}.
     *
     * <p><b>Note:</b> This collector does not limit parallelism in any way (it may spawn work for every
     * element). As a result, it is not suitable for processing huge streams.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo(i), toList()));
     * }</pre>
     *
     * @param mapper    transformation applied to each element
     * @param collector the {@code Collector} describing the reduction
     * @param <T>       the input element type
     * @param <R>       the type produced by {@code mapper}
     * @param <RR>      the reduction result type produced by {@code collector}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} of the reduced result
     *
     * @since 4.0.0
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper,
      Collector<R, ?, RR> collector) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(collector, "collector cannot be null");

        return Factory.collecting(s -> s.collect(collector), mapper, c -> {});
    }

    /**
     * A convenience {@link Collector} that performs parallel computations using Virtual Threads
     * and returns a {@link CompletableFuture} containing a {@link Stream} of the mapped results.
     * <p>
     * Each element is transformed using the provided {@code mapper} in parallel on Virtual Threads.
     * Unlike {@link #parallel(Function, Collector)}, this overload does not perform a reduction; it
     * simply collects and exposes the mapped elements as a {@code Stream} in the resulting future.
     *
     * <p><b>Note:</b> This collector does not limit parallelism in any way (it may spawn work for every
     * element). As a result, it is not suitable for processing huge streams.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo(i)));
     *
     * result.thenAccept(s -> s.forEach(System.out::println));
     * }</pre>
     *
     * @param mapper transformation applied to each element
     * @param <T>    the input element type
     * @param <R>    the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code CompletableFuture} of a {@code Stream} of mapped elements
     *
     * @since 3.0.0
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");

        return Factory.collecting((Function<Stream<R>, Stream<R>>) s -> s, mapper, c -> {});
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input
     * elements using the provided {@code classifier}, applying the given {@code mapper}, and
     * emitting {@link Group} entries representing each group.
     * <p>
     * Each element is classified using {@code classifier}, then transformed using {@code mapper} in
     * parallel on Virtual Threads. The resulting {@link Group} entries are exposed as a
     * {@link CompletableFuture} of {@code Stream<Group<K, R>>}.
     *
     * <p><b>Note:</b> This collector does not limit parallelism in any way (it may spawn work for every
     * element). As a result, it is not suitable for processing huge streams.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Stream<Group<String, String>>> result = Stream.of(t1, t2, t3)
     *   .collect(parallelBy(Task::groupId, t -> compute(t)));
     * }</pre>
     *
     * @param classifier function that assigns a grouping key to each element
     * @param mapper     transformation applied to each element
     * @param <T>        the input element type
     * @param <K>        the classification key type
     * @param <R>        the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} of a {@code Stream} of grouped results
     *
     * @since 3.4.0
     */
    public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Group<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(classifier, "classifier cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");

        return Factory.collectingBy(classifier, mapper, c -> {});
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input
     * elements using the provided {@code classifier}, applying the given {@code mapper}, and
     * emitting {@link Group} entries representing each group.
     * <p>
     * The generated {@link Stream} of {@code Group<K, R>} instances is then reduced using the
     * user-provided {@code collector}, executed on Virtual Threads. Each group is processed
     * independently, and every group is guaranteed to be processed on a single thread.
     * The reduction is applied to the grouped results rather than to the raw mapped elements.
     *
     * <p><b>Note:</b> This collector does not limit parallelism in any way (it may spawn work for every
     * element). As a result, it is not suitable for processing huge streams.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<List<Group<String, String>>> result = Stream.of(t1, t2, t3)
     *   .collect(parallelBy(Task::groupId, t -> compute(t), toList()));
     * }</pre>
     *
     * @param classifier function that assigns a grouping key to each element
     * @param mapper     transformation applied to each element
     * @param collector  the {@code Collector} describing the reduction of the grouped results
     * @param <T>        the input element type
     * @param <K>        the classification key type
     * @param <R>        the type produced by {@code mapper}
     * @param <RR>       the reduction result type produced by {@code collector}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} whose value is obtained
     * by reducing the {@code Stream<Group<K, R>>} produced by the parallel classification
     *
     * @since 3.4.0
     */
    public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Collector<Group<K, R>, ?, RR> collector) {
        Objects.requireNonNull(classifier, "classifier cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(collector, "collector cannot be null");

        return Factory.collectingBy(
          classifier,
          (Function<Stream<Group<K, R>>, RR>) s -> s.collect(collector),
          mapper,
          c -> {});
    }

    /**
     * A convenience {@link Collector} that performs parallel computations using Virtual Threads
     * and returns a {@link Stream} of the mapped results.
     * <p>
     * Each element is transformed using the provided {@code mapper} in parallel on Virtual Threads,
     * and the mapped elements are exposed as a {@code Stream}.
     *
     * <p><b>Ordering:</b> This collector emits elements in an <em>arbitrary</em> order. To preserve encounter
     * order, use the {@link StreamingConfigurer} overload and configure {@link StreamingConfigurer#ordered()}.
     *
     * <p><b>Note:</b> This collector does not limit parallelism in any way (it may spawn work for every
     * element). As a result, it is not suitable for processing huge streams.
     *
     * <br>
     * Example:
     * <pre>{@code
     * Stream<String> result = Stream.of(1, 2, 3)
     *   .collect(parallelToStream(i -> foo(i)));
     * }</pre>
     *
     * @param mapper transformation applied to each element
     * @param <T>    the input element type
     * @param <R>    the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code Stream} of mapped elements
     *
     * @since 3.0.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");

        return Factory.streaming(mapper, c -> {});
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input
     * elements using the provided {@code classifier}, applying the given {@code mapper}, and
     * emitting {@link Group} entries representing each group.
     * <p>
     * Each element is classified using {@code classifier}, then transformed using {@code mapper} in
     * parallel on Virtual Threads. The resulting grouped entries are exposed as a
     * {@code Stream<Group<K, R>>}.
     *
     * <p><b>Ordering:</b> This collector emits {@link Group} elements in an <em>arbitrary</em> order.
     * To preserve encounter order, use the {@link StreamingConfigurer} overload and configure
     * {@link StreamingConfigurer#ordered()}.
     *
     * <p><b>Note:</b> This collector does not limit parallelism in any way (it may spawn work for every
     * element). As a result, it is not suitable for processing huge streams.
     *
     * <br>
     * Example:
     * <pre>{@code
     * Stream<Group<String, String>> result = Stream.of(t1, t2, t3)
     *   .collect(parallelToStreamBy(Task::groupId, t -> compute(t)));
     * }</pre>
     *
     * @param classifier function that assigns a grouping key to each element
     * @param mapper     transformation applied to each element
     * @param <T>        the input element type
     * @param <K>        the classification key type
     * @param <R>        the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code Stream} of grouped results
     *
     * @since 3.4.0
     */
    public static <T, K, R> Collector<T, ?, Stream<Group<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(classifier, "classifier cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");

        return Factory.streamingBy(classifier, mapper, c -> {});
    }

    // configurers

    /**
     * A convenience {@link Collector} that performs parallel computations (by default on Virtual Threads)
     * and returns a {@link CompletableFuture} containing a {@link Stream} of the mapped results,
     * with additional configuration applied via the provided {@code configurer}.
     * <p>
     * Each element is transformed using the provided {@code mapper} in parallel. Unless overridden via
     * {@link CollectingConfigurer#executor(java.util.concurrent.Executor)}, tasks are executed on
     * Virtual Threads. The {@code configurer} can also be used to enable batching and/or set a maximum
     * parallelism level.
     *
     * <p><b>Note:</b> Unless the {@code configurer} explicitly limits parallelism (e.g. via
     * {@link CollectingConfigurer#parallelism(int)}), this collector does not limit parallelism in any
     * way (it may spawn work for every element). As a result, it is not suitable for processing huge
     * streams.
     *
     * <p><b>Note:</b> For more information on available configuration options, see
     * {@link CollectingConfigurer}.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo(i), c -> c
     *     .parallelism(64)
     *     .batching()
     *   ));
     * }</pre>
     *
     * @param mapper     transformation applied to each element
     * @param configurer callback used to configure execution (see {@link CollectingConfigurer})
     * @param <T>        the input element type
     * @param <R>        the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code CompletableFuture} of a {@code Stream} of mapped elements
     *
     * @since 4.0.0
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(configurer, "configurer cannot be null");

        return Factory.collecting((Function<Stream<R>, Stream<R>>) i -> i, mapper, configurer);
    }

    /**
     * A convenience {@link Collector} that performs parallel computations (by default on Virtual Threads)
     * and returns a {@link CompletableFuture} containing the result of applying the user-provided
     * {@link Collector} to the mapped elements, with additional configuration applied via the provided
     * {@code configurer}.
     * <p>
     * Each element is transformed using the provided {@code mapper} in parallel, and the results are
     * reduced according to the supplied {@code collector}. Unless overridden via
     * {@link CollectingConfigurer#executor(java.util.concurrent.Executor)}, tasks are executed on
     * Virtual Threads. The {@code configurer} can also be used to enable batching and/or limit the
     * maximum parallelism.
     *
     * <p><b>Note:</b> Unless the {@code configurer} explicitly limits parallelism (e.g. via
     * {@link CollectingConfigurer#parallelism(int)}), this collector does not limit parallelism in any
     * way (it may spawn work for every element). As a result, it is not suitable for processing huge
     * streams.
     *
     * <p><b>Note:</b> For more information on available configuration options, see
     * {@link CollectingConfigurer}.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo(i), c -> c
     *       .parallelism(64)
     *       .batching(),
     *     toList()
     *   ));
     * }</pre>
     *
     * @param mapper     transformation applied to each element
     * @param collector  the {@code Collector} describing the reduction
     * @param configurer callback used to configure execution (see {@link CollectingConfigurer})
     * @param <T>        the input element type
     * @param <R>        the type produced by {@code mapper}
     * @param <RR>       the reduction result type produced by {@code collector}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} of the reduced result
     *
     * @since 4.0.0
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer,
      Collector<R, ?, RR> collector) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(collector, "collector cannot be null");
        Objects.requireNonNull(configurer, "configurer cannot be null");

        return Factory.collecting(s -> s.collect(collector), mapper, configurer);
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input
     * elements using the provided {@code classifier}, applying the given {@code mapper}, and
     * emitting {@link Group} entries representing each group, with additional configuration applied
     * via the provided {@code configurer}.
     * <p>
     * Each element is classified using {@code classifier}, then transformed using {@code mapper} in
     * parallel. Unless overridden via {@link CollectingConfigurer#executor(java.util.concurrent.Executor)},
     * tasks are executed on Virtual Threads. The resulting grouped entries are exposed as a
     * {@link CompletableFuture} of {@code Stream<Group<K, R>>}. The {@code configurer} can also be used
     * to enable batching and/or set a maximum parallelism level.
     *
     * <p><b>Note:</b> Unless the {@code configurer} explicitly limits parallelism (e.g. via
     * {@link CollectingConfigurer#parallelism(int)}), this collector does not limit parallelism in any
     * way (it may spawn work for every element). As a result, it is not suitable for processing huge
     * streams.
     *
     * <p><b>Note:</b> For more information on available configuration options, see
     * {@link CollectingConfigurer}.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Stream<Group<String, String>>> result = Stream.of(t1, t2, t3)
     *   .collect(parallelBy(Task::groupId, t -> compute(t), c -> c
     *     .parallelism(64)
     *     .batching()
     *   ));
     * }</pre>
     *
     * @param classifier function that assigns a grouping key to each element
     * @param mapper     transformation applied to each element
     * @param configurer callback used to configure execution (see {@link CollectingConfigurer})
     * @param <T>        the input element type
     * @param <K>        the classification key type
     * @param <R>        the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} of a {@code Stream} of grouped results
     *
     * @since 4.0.0
     */
    public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Group<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer) {
        Objects.requireNonNull(classifier, "classifier cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(configurer, "configurer cannot be null");

        return Factory.collectingBy(classifier, mapper, configurer);
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input
     * elements using the provided {@code classifier}, applying the given {@code mapper}, and
     * emitting {@link Group} entries representing each group, and then reducing them using the
     * user-provided {@code collector}, with additional configuration applied via the provided
     * {@code configurer}.
     * <p>
     * The generated {@link Stream} of {@code Group<K, R>} instances is reduced using the supplied
     * {@code collector}, executed on Virtual Threads by default (unless overridden via
     * {@link CollectingConfigurer#executor(java.util.concurrent.Executor)}). Each group is processed
     * independently, and every group is guaranteed to be processed on a single thread.
     * The reduction is applied to the grouped results rather than to the raw mapped elements.
     * The {@code configurer} can also be used to enable batching and/or set a maximum parallelism level.
     *
     * <p><b>Note:</b> Unless the {@code configurer} explicitly limits parallelism (e.g. via
     * {@link CollectingConfigurer#parallelism(int)}), this collector does not limit parallelism in any
     * way (it may spawn work for every element). As a result, it is not suitable for processing huge
     * streams.
     *
     * <p><b>Note:</b> For more information on available configuration options, see
     * {@link CollectingConfigurer}.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<List<Group<String, String>>> result = Stream.of(t1, t2, t3)
     *   .collect(parallelBy(Task::groupId, t -> compute(t), c -> c
     *       .parallelism(64)
     *       .batching(),
     *     toList()
     *   ));
     * }</pre>
     *
     * @param classifier function that assigns a grouping key to each element
     * @param mapper     transformation applied to each element
     * @param configurer callback used to configure execution (see {@link CollectingConfigurer})
     * @param collector  the {@code Collector} describing the reduction for grouped results
     * @param <T>        the input element type
     * @param <K>        the classification key type
     * @param <R>        the type produced by {@code mapper}
     * @param <RR>       the reduction result type produced by {@code collector}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} whose value is obtained by
     * reducing the {@code Stream<Group<K, R>>} produced by the parallel classification
     *
     * @since 4.0.0
     */
    public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer,
      Collector<Group<K, R>, ?, RR> collector) {
        Objects.requireNonNull(classifier, "classifier cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(configurer, "configurer cannot be null");
        Objects.requireNonNull(collector, "collector cannot be null");

        return Factory.collectingBy(
          classifier,
          (Function<Stream<Group<K, R>>, RR>) s -> s.collect(collector),
          mapper,
          configurer);
    }

    /**
     * A convenience {@link Collector} that performs parallel computations (by default on Virtual Threads)
     * and returns a {@link Stream} of the mapped results, with additional configuration applied via the
     * provided {@code configurer}.
     * <p>
     * Each element is transformed using the provided {@code mapper} in parallel. Unless overridden via
     * {@link StreamingConfigurer#executor(java.util.concurrent.Executor)}, tasks are executed on Virtual
     * Threads. The {@code configurer} can also be used to enable batching and/or cap parallelism.
     *
     * <p><b>Ordering:</b> By default, this collector emits elements in an <em>arbitrary</em> order.
     * To preserve encounter order, configure ordered emission via {@link StreamingConfigurer#ordered()}.
     *
     * <p><b>Note:</b> Unless the {@code configurer} explicitly limits parallelism (e.g. via
     * {@link StreamingConfigurer#parallelism(int)}), this collector does not limit parallelism in any
     * way (it may spawn work for every element). As a result, it is not suitable for processing huge
     * streams.
     *
     * <p><b>Note:</b> For more information on available configuration options, see
     * {@link StreamingConfigurer}.
     *
     * <br>
     * Example:
     * <pre>{@code
     * Stream<String> result = Stream.of(1, 2, 3)
     *   .collect(parallelToStream(i -> foo(i), c -> c
     *     .ordered()
     *     .parallelism(64)
     *     .batching()
     *   ));
     * }</pre>
     *
     * @param mapper     transformation applied to each element
     * @param configurer callback used to configure execution (see {@link StreamingConfigurer})
     * @param <T>        the input element type
     * @param <R>        the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code Stream} of mapped elements
     *
     * @since 4.0.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper,
      Consumer<StreamingConfigurer> configurer) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(configurer, "configurer cannot be null");

        return Factory.streaming(mapper, configurer);
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input
     * elements using the provided {@code classifier}, applying the given {@code mapper}, and
     * emitting {@link Group} entries representing each group, with additional configuration applied
     * via the provided {@code configurer}.
     * <p>
     * Each element is classified using {@code classifier}, then transformed using {@code mapper} in
     * parallel. Unless overridden via {@link StreamingConfigurer#executor(java.util.concurrent.Executor)},
     * tasks are executed on Virtual Threads. The resulting grouped entries are exposed as a
     * {@code Stream<Group<K, R>>}.
     *
     * <p><b>Ordering:</b> By default, this collector emits {@link Group} elements in an
     * <em>arbitrary</em> order. To preserve encounter order, configure ordered emission via
     * {@link StreamingConfigurer#ordered()}.
     *
     * <p><b>Note:</b> Unless the {@code configurer} explicitly limits parallelism (e.g. via
     * {@link StreamingConfigurer#parallelism(int)}), this collector does not limit parallelism in any
     * way (it may spawn work for every element). As a result, it is not suitable for processing huge
     * streams.
     *
     * <p><b>Note:</b> For more information on available configuration options, see
     * {@link StreamingConfigurer}.
     *
     * <br>
     * Example:
     * <pre>{@code
     * Stream<Group<String, String>> result = Stream.of(t1, t2, t3)
     *   .collect(parallelToStreamBy(Task::groupId, t -> compute(t), c -> c
     *     .ordered()
     *     .parallelism(64)
     *     .batching()
     *   ));
     * }</pre>
     *
     * @param classifier function that assigns a grouping key to each element
     * @param mapper     transformation applied to each element
     * @param configurer callback used to configure execution (see {@link StreamingConfigurer})
     * @param <T>        the input element type
     * @param <K>        the classification key type
     * @param <R>        the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code Stream} of grouped results
     *
     * @since 4.0.0
     */
    public static <T, K, R> Collector<T, ?, Stream<Group<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Consumer<StreamingConfigurer> configurer) {
        Objects.requireNonNull(classifier, "classifier cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(configurer, "configurer cannot be null");

        return Factory.streamingBy(classifier, mapper, configurer);
    }

    // convenience (defaults + parallelism)

    /**
     * A convenience {@link Collector} that performs parallel computations using Virtual Threads
     * and returns a {@link CompletableFuture} containing a {@link Stream} of the mapped results.
     * <p>
     * This overload is a convenience for applying an easy parallelism cap. For additional configuration
     * options (e.g. batching or a custom {@link java.util.concurrent.Executor}), use the overload
     * accepting a {@link CollectingConfigurer}.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Stream<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo(i), 64));
     * }</pre>
     *
     * @param mapper      transformation applied to each element
     * @param parallelism maximum parallelism (must be positive)
     * @param <T>         the input element type
     * @param <R>         the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code CompletableFuture} of a {@code Stream} of mapped elements
     *
     * @since 4.0.0
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper, int parallelism) {
        Objects.requireNonNull(mapper, "mapper cannot be null");

        return Factory.collecting(
          (Function<Stream<R>, Stream<R>>) i -> i,
          mapper,
          c -> c.parallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} that performs parallel computations using Virtual Threads
     * and returns a {@link CompletableFuture} containing the reduced result produced by the provided
     * {@code collector}.
     * <p>
     * This overload is a convenience for applying an easy parallelism cap. For additional configuration
     * options (e.g. batching or a custom {@link java.util.concurrent.Executor}), use the overload
     * accepting a {@link CollectingConfigurer}.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<List<String>> result = Stream.of(1, 2, 3)
     *   .collect(parallel(i -> foo(i), 64, toList()));
     * }</pre>
     *
     * @param mapper      transformation applied to each element
     * @param parallelism maximum parallelism (must be positive)
     * @param collector   the {@code Collector} describing the reduction
     * @param <T>         the input element type
     * @param <R>         the type produced by {@code mapper}
     * @param <RR>        the reduction result type produced by {@code collector}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} of the reduced result
     *
     * @since 4.0.0
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper,
      int parallelism, Collector<R, ?, RR> collector) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(collector, "collector cannot be null");

        return Factory.collecting(
          (Function<Stream<R>, RR>) s -> s.collect(collector),
          mapper,
          c -> c.parallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input elements
     * using the provided {@code classifier}, applying the given {@code mapper}, and emitting
     * {@link Group} entries representing each batch.
     * <p>
     * This overload is a convenience for applying an easy parallelism cap. For additional configuration
     * options (e.g. batching or a custom {@link java.util.concurrent.Executor}), use the overload
     * accepting a {@link CollectingConfigurer}.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<Stream<Group<String, String>>> result = Stream.of(t1, t2, t3)
     *   .collect(parallelBy(Task::groupId, t -> compute(t), 64));
     * }</pre>
     *
     * @param classifier  function that groups elements into batches
     * @param mapper      transformation applied to each element
     * @param parallelism maximum parallelism (must be positive)
     * @param <T>         the input element type
     * @param <K>         the classification key type
     * @param <R>         the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} of a {@code Stream} of grouped results
     *
     * @since 4.0.0
     */
    public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Group<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper, int parallelism) {
        Objects.requireNonNull(classifier, "classifier cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");

        return Factory.collectingBy(classifier, mapper, c -> c.parallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input elements
     * using the provided {@code classifier}, applying the given {@code mapper}, emitting {@link Group}
     * entries representing each batch, and then reducing them using the user-provided {@code collector}.
     * <p>
     * This overload is a convenience for applying an easy parallelism cap. For additional configuration
     * options (e.g. batching or a custom {@link java.util.concurrent.Executor}), use the overload
     * accepting a {@link CollectingConfigurer}.
     *
     * <br>
     * Example:
     * <pre>{@code
     * CompletableFuture<List<Group<String, String>>> result = Stream.of(t1, t2, t3)
     *   .collect(parallelBy(Task::groupId, t -> compute(t), 64, toList()));
     * }</pre>
     *
     * @param classifier  function that groups elements into batches
     * @param mapper      transformation applied to each element
     * @param parallelism maximum parallelism (must be positive)
     * @param collector   the {@code Collector} describing the reduction for grouped results
     * @param <T>         the input element type
     * @param <K>         the classification key type
     * @param <R>         the type produced by {@code mapper}
     * @param <RR>        the reduction result type produced by {@code collector}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} of the reduced result
     *
     * @since 4.0.0
     */
    public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper, int parallelism,
      Collector<Group<K, R>, ?, RR> collector) {
        Objects.requireNonNull(classifier, "classifier cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(collector, "collector cannot be null");

        return Factory.collectingBy(
          classifier,
          (Function<Stream<Group<K, R>>, RR>) s -> s.collect(collector),
          mapper,
          c -> c.parallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} that performs parallel computations using Virtual Threads
     * and returns a {@link Stream} of the mapped results.
     * <p>
     * This overload is a convenience for applying an easy parallelism cap. This method does not expose
     * additional configuration (such as ordered emission, batching, or a custom executor). For more
     * options, use the overload accepting a {@link StreamingConfigurer}.
     *
     * <p><b>Ordering:</b> This collector emits elements in an <em>arbitrary</em> order. To preserve encounter
     * order, use the {@link StreamingConfigurer} overload and configure {@link StreamingConfigurer#ordered()}.
     *
     * <br>
     * Example:
     * <pre>{@code
     * Stream<String> result = Stream.of(1, 2, 3)
     *   .collect(parallelToStream(i -> foo(i), 64));
     * }</pre>
     *
     * @param mapper      transformation applied to each element
     * @param parallelism maximum parallelism (must be positive)
     * @param <T>         the input element type
     * @param <R>         the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code Stream} of mapped elements
     *
     * @since 4.0.0
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper, int parallelism) {
        Objects.requireNonNull(mapper, "mapper cannot be null");

        return Factory.streaming(mapper, c -> c.parallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input elements
     * using the provided {@code classifier}, applying the given {@code mapper}, and emitting
     * {@link Group} entries representing each batch.
     * <p>
     * This overload is a convenience for applying an easy parallelism cap. This method does not expose
     * additional configuration (such as ordered emission, batching, or a custom executor). For more
     * options, use the overload accepting a {@link StreamingConfigurer}.
     *
     * <p><b>Ordering:</b> This collector emits {@link Group} elements in an <em>arbitrary</em> order.
     * To preserve encounter order, use the {@link StreamingConfigurer} overload and configure
     * {@link StreamingConfigurer#ordered()}.
     *
     * <br>
     * Example:
     * <pre>{@code
     * Stream<Group<String, String>> result = Stream.of(t1, t2, t3)
     *   .collect(parallelToStreamBy(Task::groupId, t -> compute(t), 64));
     * }</pre>
     *
     * @param classifier  function that groups elements into batches
     * @param mapper      transformation applied to each element
     * @param parallelism maximum parallelism (must be positive)
     * @param <T>         the input element type
     * @param <K>         the classification key type
     * @param <R>         the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code Stream} of grouped results
     *
     * @since 4.0.0
     */
    public static <T, K, R> Collector<T, ?, Stream<Group<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper, int parallelism) {
        Objects.requireNonNull(classifier, "classifier cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");

        return Factory.streamingBy(classifier, mapper, c -> c.parallelism(parallelism));
    }

    /**
     * A convenience {@code Collector} for collecting a {@code Stream<CompletableFuture<T>>}
     * into a {@code CompletableFuture<R>} using a provided {@code Collector<T, ?, R>}
     *
     * @param collector the {@code Collector} describing the reduction
     * @param <T>       the type of the collected elements
     * @param <R>       the result type of the downstream {@code Collector}
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
}
