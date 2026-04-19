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

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

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
        return null;
    }

    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper) {
        return null;
    }

    public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Group<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper) {
        return null;
    }

    public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Collector<Group<K, R>, ?, RR> collector) {
        return null;
    }

    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper) {
        return null;
    }

    public static <T, K, R> Collector<T, ?, Stream<Group<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper) {
        return null;
    }

    // configurers

    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer) {
        return null;
    }

    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer,
      Collector<R, ?, RR> collector) {
        return null;
    }

    public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Group<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer) {
        return null;
    }

    public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Consumer<CollectingConfigurer> configurer,
      Collector<Group<K, R>, ?, RR> collector) {
        return null;
    }

    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper,
      Consumer<StreamingConfigurer> configurer) {
        return null;
    }

    public static <T, K, R> Collector<T, ?, Stream<Group<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Consumer<StreamingConfigurer> configurer) {
        return null;
    }

    // convenience (defaults + parallelism)

    public static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> parallel(
      Function<? super T, ? extends R> mapper, int parallelism) {
        return null;
    }

    public static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> parallel(
      Function<? super T, ? extends R> mapper,
      int parallelism, Collector<R, ?, RR> collector) {
        return null;
    }

    public static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Group<K, R>>>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper, int parallelism) {
        return null;
    }

    public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper, int parallelism,
      Collector<Group<K, R>, ?, RR> collector) {
        return null;
    }

    public static <T, R> Collector<T, ?, Stream<R>> parallelToStream(
      Function<? super T, ? extends R> mapper, int parallelism) {
        return null;
    }

    public static <T, K, R> Collector<T, ?, Stream<Group<K, R>>> parallelToStreamBy(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper, int parallelism) {
        return null;
    }
}
