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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompletionStrategyTest {

    @Test
    void shouldHaveOrderedValue() {
        assertThat(CompletionStrategy.ORDERED).isNotNull();
    }

    @Test
    void shouldHaveUnorderedValue() {
        assertThat(CompletionStrategy.UNORDERED).isNotNull();
    }

    @Test
    void shouldHaveExactlyTwoValues() {
        assertThat(CompletionStrategy.values()).hasSize(2);
    }

    @Test
    void shouldContainBothValues() {
        assertThat(CompletionStrategy.values())
          .containsExactlyInAnyOrder(CompletionStrategy.ORDERED, CompletionStrategy.UNORDERED);
    }

    @Test
    void shouldRetrieveValueByName() {
        assertThat(CompletionStrategy.valueOf("ORDERED")).isEqualTo(CompletionStrategy.ORDERED);
        assertThat(CompletionStrategy.valueOf("UNORDERED")).isEqualTo(CompletionStrategy.UNORDERED);
    }

    @Test
    void shouldHaveDistinctValues() {
        assertThat(CompletionStrategy.ORDERED).isNotEqualTo(CompletionStrategy.UNORDERED);
    }
}
