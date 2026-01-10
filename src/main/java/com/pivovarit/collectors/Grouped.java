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
import java.util.function.BiFunction;

/**
 * Represents a grouping of values under a specific key.
 *
 * @param <T>    the type of the key
 * @param <V>    the type of the values
 * @param key    the key of this group, must not be null
 * @param values the list of values, must not be null
 */
public record Grouped<T, V>(T key, List<V> values) {

    /**
     * Constructs a new {@code Grouped} instance ensuring key and values are not null.
     *
     * @param key    the key, must not be null
     * @param values the list of values, must not be null
     */
    public Grouped {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(values, "values cannot be null");
    }

    /**
     * Creates a new {@code Grouped} instance with the given key and values.
     *
     * @param key    the key, must not be null
     * @param values the list of values, must not be null
     * @param <T>    the type of the key
     * @param <V>    the type of the values
     *
     * @return a new {@code Grouped} instance
     */
    public static <T, V> Grouped<T, V> of(T key, List<V> values) {
        return new Grouped<>(key, values);
    }

    /**
     * Transforms the values in this group using the provided mapper function.
     *
     * @param mapper the mapping function, must not be null
     * @param <R>    the target type of the mapped values
     *
     * @return a new {@code Grouped} instance with the same key and the values produced by applying
     * {@code mapper} to each element in this group's values
     */
    public <R> Grouped<T, R> map(BiFunction<? super T, ? super V, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        return new Grouped<>(key, values.stream()
          .map(v -> mapper.apply(key, v))
          .map(a -> (R) a)
          .toList());
    }
}
