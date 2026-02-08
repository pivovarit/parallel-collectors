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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupTest {

    @Nested
    class ConstructorTest {

        @Test
        void constructorShouldRejectNullKey() {
            assertThatThrownBy(() -> new Group<>(null, List.of(1, 2)))
              .isInstanceOf(NullPointerException.class)
              .hasMessage("key cannot be null");
        }

        @Test
        void constructorShouldRejectNullValues() {
            assertThatThrownBy(() -> new Group<>("key", null))
              .isInstanceOf(NullPointerException.class)
              .hasMessage("values cannot be null");
        }
    }

    @Nested
    class FactoryTest {
        @Test
        void ofShouldCreateInstance() {
            Group<String, Integer> g = Group.of("k", List.of(1, 2));

            assertThat(g.key()).isEqualTo("k");
            assertThat(g.values()).containsExactly(1, 2);
        }
    }

    @Nested
    class MapTest {

        @Test
        void mapShouldApplyMapperToAllValues() {
            var g = new Group<>("numbers", List.of(1, 2, 3));

            assertThat(g.map((k, i) -> "n" + i)).isEqualTo(Group.of("numbers", List.of("n1", "n2", "n3")));
        }

        @Test
        void mapShouldWorkOnEmptyValuesList() {
            var g = new Group<>("empty", List.of());

            assertThat(g.map((k, o) -> o.toString())).isEqualTo(new Group<>("empty", List.of()));
        }

        @Test
        void mapShouldReturnNewInstanceAndNotMutateOriginal() {
            Group<String, Integer> g = new Group<>("x", List.of(1, 2));

            Group<String, Integer> mapped = g.map((k, i) -> i * 10);

            assertThat(g.values()).containsExactly(1, 2);
            assertThat(mapped.values()).containsExactly(10, 20);
            assertThat(mapped).isNotSameAs(g);
        }

        @Test
        void mapShouldRejectNullMapper() {
            Group<String, Integer> g = new Group<>("k", List.of(1));

            assertThatThrownBy(() -> g.map(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
