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

public class Group<K, R> {

    private final K key;
    private final List<R> values;

    Group(K key, List<R> values) {
        this.key = key;
        this.values = values;
    }

    public static Group<Integer, Integer> of(int i, List<Integer> integers) {
        return new Group<>(i, integers);
    }

    public List<R> values() {
        return values;
    }

    K key() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Group<?, ?> other)) {
            return false;
        }
        return Objects.equals(key, other.key) && Objects.equals(values, other.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, values);
    }

    @Override
    public String toString() {
        return "Group[key=" + key + ", values=" + values + "]";
    }
}
