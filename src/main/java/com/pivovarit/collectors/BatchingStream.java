package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
final class BatchingStream<T> implements Spliterator<List<T>> {

    private final List<T> source;
    private final int size;
    private final int maxChunks;

    private int chunks;
    private int chunkSize;
    private int leftElements;
    private int i;

    private BatchingStream(List<T> list, int numberOfParts) {
        source = list;
        size = list.size();
        chunks = numberOfParts;
        maxChunks = numberOfParts;
        chunkSize = (int) Math.ceil(((double) size) / numberOfParts);
        leftElements = size;
    }

    static <T> Stream<List<T>> partitioned(List<T> list, int numberOfParts) {
        int size = list.size();

        if (size == numberOfParts) {
            return asSingletonListStream(list);
        } else if (size == 0 || numberOfParts == 0) {
            return empty();
        } else if (numberOfParts == 1) {
            return of(list);
        } else {
            return stream(new BatchingStream<>(list, numberOfParts), false);
        }
    }

    private static <T> Stream<List<T>> asSingletonListStream(List<T> list) {
        Stream.Builder<List<T>> acc = Stream.builder();
        for (T t : list) {
            acc.add(Collections.singletonList(t));
        }
        return acc.build();
    }

    static <T, R> Function<List<T>, List<R>> batching(Function<T, R> mapper) {
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
        if (i < size && chunks != 0) {
            List<T> batch = source.subList(i, i + chunkSize);
            i = i + chunkSize;
            leftElements = leftElements - chunkSize;
            chunkSize = (int) Math.ceil(((double) leftElements) / --chunks);
            action.accept(batch);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Spliterator<List<T>> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return maxChunks;
    }

    @Override
    public int characteristics() {
        return ORDERED | SIZED;
    }
}
