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

    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper,
      Collector<R, ?, RR> collector) {
        return Factory.async(mapper, ConfigProcessor.empty(), collector);
    }

    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper) {
        return Factory.async(mapper, ConfigProcessor.empty());
    }

    public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Group<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper) {
        return Factory.asyncGrouping(classifier, mapper, ConfigProcessor.empty());
    }

    public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Collector<Group<K, R>, ?, RR> collector) {
        return Factory.asyncGrouping(classifier, mapper, ConfigProcessor.empty(), collector);
    }

    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper) {
        return Factory.streaming(mapper, ConfigProcessor.empty());
    }

    public static <T, K, R> Collector<T, ?, Stream<Group<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper) {
        return Factory.streamingGrouping(classifier, mapper, ConfigProcessor.empty());
    }

    // configurers

    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer) {
        return Factory.async(mapper, ConfigProcessor.fromCollecting(configurer));
    }

    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer,
      Collector<R, ?, RR> collector) {
        return Factory.async(mapper, ConfigProcessor.fromCollecting(configurer), collector);
    }

    public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Group<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer) {
        return Factory.asyncGrouping(classifier, mapper, ConfigProcessor.fromCollecting(configurer));
    }

    public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer,
      Collector<Group<K, R>, ?, RR> collector) {
        return Factory.asyncGrouping(classifier, mapper, ConfigProcessor.fromCollecting(configurer), collector);
    }

    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper,
      Consumer<StreamingConfigurer> configurer) {
        return Factory.streaming(mapper, ConfigProcessor.fromStreaming(configurer));
    }

    public static <T, K, R> Collector<T, ?, Stream<Group<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Consumer<StreamingConfigurer> configurer) {
        return Factory.streamingGrouping(classifier, mapper, ConfigProcessor.fromStreaming(configurer));
    }

    // convenience (defaults + parallelism)

    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper, int parallelism) {
        return Factory.async(mapper, ConfigProcessor.collectingWithParallelism(parallelism));
    }

    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper,
      int parallelism, Collector<R, ?, RR> collector) {
        return Factory.async(mapper, ConfigProcessor.collectingWithParallelism(parallelism), collector);
    }

    public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Group<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper, int parallelism) {
        return Factory.asyncGrouping(classifier, mapper, ConfigProcessor.collectingWithParallelism(parallelism));
    }

    public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper, int parallelism,
      Collector<Group<K, R>, ?, RR> collector) {
        return Factory.asyncGrouping(classifier, mapper, ConfigProcessor.collectingWithParallelism(parallelism), collector);
    }

    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper, int parallelism) {
        return Factory.streaming(mapper, ConfigProcessor.streamingWithParallelism(parallelism));
    }

    public static <T, K, R> Collector<T, ?, Stream<Group<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper, int parallelism) {
        return Factory.streamingGrouping(classifier, mapper, ConfigProcessor.streamingWithParallelism(parallelism));
    }
}
