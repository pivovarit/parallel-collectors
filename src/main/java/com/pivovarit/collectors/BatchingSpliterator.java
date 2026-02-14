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
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static java.util.stream.StreamSupport.stream;

/**
 * @author Grzegorz Piwowarek
 */
final class BatchingSpliterator<T> implements Spliterator<List<T>> {

    private final List<T> source;

    private int chunks;
    private int chunkSize;
    private int consumed;

    BatchingSpliterator(List<T> list, int batches) {
        Objects.requireNonNull(list, "list can't be null");
        if (batches < 1) {
            throw new IllegalArgumentException("batches can't be lower than one");
        }
        source = list;
        chunks = list.isEmpty() ? 0 : Math.min(batches, list.size());
        chunkSize = (int) Math.ceil(((double) source.size()) / batches);
    }

    static <T> Stream<List<T>> partitioned(List<T> list, int numberOfParts) {
        int size = list.size();

        if (size == 0) {
            return empty();
        } else if (numberOfParts == 1) {
            return of(list);
        } else if (size <= numberOfParts) {
            return asSingletonListStream(list);
        } else {
            return stream(new BatchingSpliterator<>(list, numberOfParts), false);
        }
    }

    private static <T> Stream<List<T>> asSingletonListStream(List<T> list) {
        Stream.Builder<List<T>> acc = Stream.builder();
        for (T t : list) {
            acc.add(List.of(t));
        }
        return acc.build();
    }

    static <T, R> Function<List<T>, List<R>> batching(Function<? super T, R> mapper) {
        return batch -> {
            List<R> list = new ArrayList<>(batch.size());
            for (T t : batch) {
                list.add(mapper.apply(t));
            }
            return list;
        };
    }

    @Override
    public boolean tryAdvance(Consumer<? super List<T>> action) {
        if (consumed < source.size() && chunks != 0) {
            int end = Math.min(source.size(), consumed + chunkSize);
            List<T> batch = source.subList(consumed, end);
            consumed += batch.size();
            chunkSize = (int) Math.ceil(((double) (source.size() - consumed)) / --chunks);
            action.accept(batch);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Spliterator<List<T>> trySplit() {
        int remaining = source.size() - consumed;
        if (remaining <= chunkSize || chunks <= 1) {
            return null;
        }

        int midChunks = chunks / 2;
        int midSize = midChunks * chunkSize;

        var subList = source.subList(consumed, consumed + midSize);
        var split = new BatchingSpliterator<>(subList, midChunks);

        consumed += midSize;
        chunks -= midChunks;
        chunkSize = (int) Math.ceil(((double) (source.size() - consumed)) / chunks);

        return split;
    }

    @Override
    public long estimateSize() {
        return chunks;
    }

    @Override
    public int characteristics() {
        return IMMUTABLE | ORDERED | SIZED;
    }
}
