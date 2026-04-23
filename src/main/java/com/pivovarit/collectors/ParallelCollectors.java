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
     * @param mapper    transformation applied to each element
     * @param collector the {@code Collector} describing the reduction
     * @param <T>       the input element type
     * @param <R>       the type produced by {@code mapper}
     * @param <RR>      the reduction result type produced by {@code collector}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} of the reduced result
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper,
      Collector<R, ?, RR> collector) {
        return Factory.async(mapper, ConfigProcessor.empty(), collector);
    }

    /**
     * A convenience {@link Collector} that performs parallel computations using Virtual Threads
     * and returns a {@link CompletableFuture} containing a {@link Stream} of the mapped results.
     *
     * <p><b>Note:</b> This collector does not limit parallelism in any way (it may spawn work for every
     * element). As a result, it is not suitable for processing huge streams.
     *
     * @param mapper transformation applied to each element
     * @param <T>    the input element type
     * @param <R>    the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code CompletableFuture} of a {@code Stream} of mapped elements
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper) {
        return Factory.async(mapper, ConfigProcessor.empty());
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input
     * elements using the provided {@code classifier}, applying the given {@code mapper}, and
     * emitting {@link Group} entries representing each group.
     *
     * <p><b>Note:</b> This collector does not limit parallelism in any way (it may spawn work for every
     * element). As a result, it is not suitable for processing huge streams.
     *
     * @param classifier function that assigns a grouping key to each element
     * @param mapper     transformation applied to each element
     * @param <T>        the input element type
     * @param <K>        the classification key type
     * @param <R>        the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} of a {@code Stream} of grouped results
     */
    public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Group<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper) {
        return Factory.asyncGrouping(classifier, mapper, ConfigProcessor.empty());
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input
     * elements using the provided {@code classifier}, applying the given {@code mapper}, and
     * reducing the resulting {@link Group} entries using the user-provided {@code collector}.
     * <p>
     * Each group is processed independently, and every group is guaranteed to be processed on
     * a single thread.
     *
     * <p><b>Note:</b> This collector does not limit parallelism in any way (it may spawn work for every
     * element). As a result, it is not suitable for processing huge streams.
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
     */
    public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Collector<Group<K, R>, ?, RR> collector) {
        return Factory.asyncGrouping(classifier, mapper, ConfigProcessor.empty(), collector);
    }

    /**
     * A convenience {@link Collector} that performs parallel computations using Virtual Threads
     * and returns a {@link Stream} of the mapped results.
     *
     * <p><b>Ordering:</b> This collector emits elements in an <em>arbitrary</em> order. To preserve encounter
     * order, use the {@link StreamingConfigurer} overload and configure {@link StreamingConfigurer#ordered()}.
     *
     * <p><b>Note:</b> This collector does not limit parallelism in any way (it may spawn work for every
     * element). As a result, it is not suitable for processing huge streams.
     *
     * @param mapper transformation applied to each element
     * @param <T>    the input element type
     * @param <R>    the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code Stream} of mapped elements
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper) {
        return Factory.streaming(mapper, ConfigProcessor.empty());
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input
     * elements using the provided {@code classifier}, applying the given {@code mapper}, and
     * emitting {@link Group} entries representing each group as a {@link Stream}.
     *
     * <p><b>Ordering:</b> This collector emits {@link Group} elements in an <em>arbitrary</em> order.
     * To preserve encounter order, use the {@link StreamingConfigurer} overload and configure
     * {@link StreamingConfigurer#ordered()}.
     *
     * <p><b>Note:</b> This collector does not limit parallelism in any way (it may spawn work for every
     * element). As a result, it is not suitable for processing huge streams.
     *
     * @param classifier function that assigns a grouping key to each element
     * @param mapper     transformation applied to each element
     * @param <T>        the input element type
     * @param <K>        the classification key type
     * @param <R>        the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code Stream} of grouped results
     */
    public static <T, K, R> Collector<T, ?, Stream<Group<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper) {
        return Factory.streamingGrouping(classifier, mapper, ConfigProcessor.empty());
    }

    // configurers

    /**
     * A convenience {@link Collector} that performs parallel computations (by default on Virtual Threads)
     * and returns a {@link CompletableFuture} containing a {@link Stream} of the mapped results,
     * with additional configuration applied via the provided {@code configurer}.
     *
     * <p><b>Note:</b> For more information on available configuration options, see
     * {@link CollectingConfigurer}.
     *
     * @param mapper     transformation applied to each element
     * @param configurer callback used to configure execution (see {@link CollectingConfigurer})
     * @param <T>        the input element type
     * @param <R>        the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code CompletableFuture} of a {@code Stream} of mapped elements
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer) {
        return Factory.async(mapper, ConfigProcessor.fromCollecting(configurer));
    }

    /**
     * A convenience {@link Collector} that performs parallel computations (by default on Virtual Threads)
     * and returns a {@link CompletableFuture} containing the result of applying the user-provided
     * {@link Collector} to the mapped elements, with additional configuration applied via the provided
     * {@code configurer}.
     *
     * <p><b>Note:</b> For more information on available configuration options, see
     * {@link CollectingConfigurer}.
     *
     * @param mapper     transformation applied to each element
     * @param configurer callback used to configure execution (see {@link CollectingConfigurer})
     * @param collector  the {@code Collector} describing the reduction
     * @param <T>        the input element type
     * @param <R>        the type produced by {@code mapper}
     * @param <RR>       the reduction result type produced by {@code collector}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} of the reduced result
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer,
      Collector<R, ?, RR> collector) {
        return Factory.async(mapper, ConfigProcessor.fromCollecting(configurer), collector);
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input
     * elements using the provided {@code classifier}, applying the given {@code mapper}, and
     * emitting {@link Group} entries representing each group, with additional configuration applied
     * via the provided {@code configurer}.
     *
     * <p><b>Note:</b> For more information on available configuration options, see
     * {@link CollectingConfigurer}.
     *
     * @param classifier function that assigns a grouping key to each element
     * @param mapper     transformation applied to each element
     * @param configurer callback used to configure execution (see {@link CollectingConfigurer})
     * @param <T>        the input element type
     * @param <K>        the classification key type
     * @param <R>        the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} of a {@code Stream} of grouped results
     */
    public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Group<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer) {
        return Factory.asyncGrouping(classifier, mapper, ConfigProcessor.fromCollecting(configurer));
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input
     * elements using the provided {@code classifier}, applying the given {@code mapper}, and
     * reducing the resulting {@link Group} entries using the user-provided {@code collector},
     * with additional configuration applied via the provided {@code configurer}.
     * <p>
     * Each group is processed independently, and every group is guaranteed to be processed on
     * a single thread.
     *
     * <p><b>Note:</b> For more information on available configuration options, see
     * {@link CollectingConfigurer}.
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
     */
    public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer,
      Collector<Group<K, R>, ?, RR> collector) {
        return Factory.asyncGrouping(classifier, mapper, ConfigProcessor.fromCollecting(configurer), collector);
    }

    /**
     * A convenience {@link Collector} that performs parallel computations (by default on Virtual Threads)
     * and returns a {@link Stream} of the mapped results, with additional configuration applied via the
     * provided {@code configurer}.
     *
     * <p><b>Ordering:</b> By default, this collector emits elements in an <em>arbitrary</em> order.
     * To preserve encounter order, configure ordered emission via {@link StreamingConfigurer#ordered()}.
     *
     * <p><b>Note:</b> For more information on available configuration options, see
     * {@link StreamingConfigurer}.
     *
     * @param mapper     transformation applied to each element
     * @param configurer callback used to configure execution (see {@link StreamingConfigurer})
     * @param <T>        the input element type
     * @param <R>        the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code Stream} of mapped elements
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper,
      Consumer<StreamingConfigurer> configurer) {
        return Factory.streaming(mapper, ConfigProcessor.fromStreaming(configurer));
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input
     * elements using the provided {@code classifier}, applying the given {@code mapper}, and
     * emitting {@link Group} entries representing each group as a {@link Stream}, with additional
     * configuration applied via the provided {@code configurer}.
     *
     * <p><b>Ordering:</b> By default, this collector emits {@link Group} elements in an
     * <em>arbitrary</em> order. To preserve encounter order, configure ordered emission via
     * {@link StreamingConfigurer#ordered()}.
     *
     * <p><b>Note:</b> For more information on available configuration options, see
     * {@link StreamingConfigurer}.
     *
     * @param classifier function that assigns a grouping key to each element
     * @param mapper     transformation applied to each element
     * @param configurer callback used to configure execution (see {@link StreamingConfigurer})
     * @param <T>        the input element type
     * @param <K>        the classification key type
     * @param <R>        the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code Stream} of grouped results
     */
    public static <T, K, R> Collector<T, ?, Stream<Group<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Consumer<StreamingConfigurer> configurer) {
        return Factory.streamingGrouping(classifier, mapper, ConfigProcessor.fromStreaming(configurer));
    }

    // convenience (defaults + parallelism)

    /**
     * A convenience {@link Collector} that performs parallel computations using Virtual Threads
     * and returns a {@link CompletableFuture} containing a {@link Stream} of the mapped results,
     * bounded by the provided parallelism level.
     *
     * @param mapper      transformation applied to each element
     * @param parallelism maximum parallelism (must be positive)
     * @param <T>         the input element type
     * @param <R>         the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code CompletableFuture} of a {@code Stream} of mapped elements
     */
    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper, int parallelism) {
        return Factory.async(mapper, ConfigProcessor.collectingWithParallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} that performs parallel computations using Virtual Threads
     * and returns a {@link CompletableFuture} containing the reduced result produced by the provided
     * {@code collector}, bounded by the provided parallelism level.
     *
     * @param mapper      transformation applied to each element
     * @param parallelism maximum parallelism (must be positive)
     * @param collector   the {@code Collector} describing the reduction
     * @param <T>         the input element type
     * @param <R>         the type produced by {@code mapper}
     * @param <RR>        the reduction result type produced by {@code collector}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} of the reduced result
     */
    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper,
      int parallelism, Collector<R, ?, RR> collector) {
        return Factory.async(mapper, ConfigProcessor.collectingWithParallelism(parallelism), collector);
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input elements
     * using the provided {@code classifier}, applying the given {@code mapper}, and emitting
     * {@link Group} entries representing each group, bounded by the provided parallelism level.
     *
     * @param classifier  function that assigns a grouping key to each element
     * @param mapper      transformation applied to each element
     * @param parallelism maximum parallelism (must be positive)
     * @param <T>         the input element type
     * @param <K>         the classification key type
     * @param <R>         the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} of a {@code Stream} of grouped results
     */
    public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Group<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper, int parallelism) {
        return Factory.asyncGrouping(classifier, mapper, ConfigProcessor.collectingWithParallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input elements
     * using the provided {@code classifier}, applying the given {@code mapper}, emitting {@link Group}
     * entries representing each group, and then reducing them using the user-provided {@code collector},
     * bounded by the provided parallelism level.
     *
     * @param classifier  function that assigns a grouping key to each element
     * @param mapper      transformation applied to each element
     * @param parallelism maximum parallelism (must be positive)
     * @param collector   the {@code Collector} describing the reduction for grouped results
     * @param <T>         the input element type
     * @param <K>         the classification key type
     * @param <R>         the type produced by {@code mapper}
     * @param <RR>        the reduction result type produced by {@code collector}
     *
     * @return a {@code Collector} producing a {@link CompletableFuture} of the reduced result
     */
    public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper, int parallelism,
      Collector<Group<K, R>, ?, RR> collector) {
        return Factory.asyncGrouping(classifier, mapper, ConfigProcessor.collectingWithParallelism(parallelism), collector);
    }

    /**
     * A convenience {@link Collector} that performs parallel computations using Virtual Threads
     * and returns a {@link Stream} of the mapped results, bounded by the provided parallelism level.
     *
     * <p><b>Ordering:</b> This collector emits elements in an <em>arbitrary</em> order. To preserve encounter
     * order, use the {@link StreamingConfigurer} overload and configure {@link StreamingConfigurer#ordered()}.
     *
     * @param mapper      transformation applied to each element
     * @param parallelism maximum parallelism (must be positive)
     * @param <T>         the input element type
     * @param <R>         the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code Stream} of mapped elements
     */
    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper, int parallelism) {
        return Factory.streaming(mapper, ConfigProcessor.streamingWithParallelism(parallelism));
    }

    /**
     * A convenience {@link Collector} that performs parallel computations by classifying input elements
     * using the provided {@code classifier}, applying the given {@code mapper}, and emitting
     * {@link Group} entries representing each group as a {@link Stream}, bounded by the provided
     * parallelism level.
     *
     * <p><b>Ordering:</b> This collector emits {@link Group} elements in an <em>arbitrary</em> order.
     * To preserve encounter order, use the {@link StreamingConfigurer} overload and configure
     * {@link StreamingConfigurer#ordered()}.
     *
     * @param classifier  function that assigns a grouping key to each element
     * @param mapper      transformation applied to each element
     * @param parallelism maximum parallelism (must be positive)
     * @param <T>         the input element type
     * @param <K>         the classification key type
     * @param <R>         the type produced by {@code mapper}
     *
     * @return a {@code Collector} producing a {@code Stream} of grouped results
     */
    public static <T, K, R> Collector<T, ?, Stream<Group<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper, int parallelism) {
        return Factory.streamingGrouping(classifier, mapper, ConfigProcessor.streamingWithParallelism(parallelism));
    }
}
