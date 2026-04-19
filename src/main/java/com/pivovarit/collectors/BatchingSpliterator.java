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

import java.util.ArrayList;
import java.util.List;

final class BatchingSpliterator {

    private BatchingSpliterator() {
    }

    static <T> List<List<T>> partition(List<T> items, int batches) {
        if (items.isEmpty()) {
            return List.of();
        }
        int count = Math.min(batches, items.size());
        int base = items.size() / count;
        int remainder = items.size() % count;
        List<List<T>> result = new ArrayList<>(count);
        int offset = 0;
        for (int i = 0; i < count; i++) {
            int size = base + (i < remainder ? 1 : 0);
            result.add(new ArrayList<>(items.subList(offset, offset + size)));
            offset += size;
        }
        return result;
    }
}
