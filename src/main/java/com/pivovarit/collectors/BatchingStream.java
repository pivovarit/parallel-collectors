package com.pivovarit.collectors;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.Runtime.getRuntime;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

final class BatchingStream<T> implements Iterator<List<T>> {

    private final List<T> source;
    private final int size;

    private int chunks;
    private int chunkSize;
    private int leftElements;
    private int i;

    private BatchingStream(List<T> list, int numberOfParts) {
        source = list;
        size = list.size();
        chunks = numberOfParts;
        chunkSize = (int) Math.ceil(((double) size) / numberOfParts);
        leftElements = size;
    }

    private static <T> Iterator<List<T>> from(List<T> source, int chunks) {
        return new BatchingStream<>(source, chunks);
    }

    static int defaultBatchAmount() {
        return Math.max(getRuntime().availableProcessors() - 1, 1);
    }

    static <T> Stream<List<T>> partitioned(List<T> list, int numberOfParts) {
        return stream(spliteratorUnknownSize(from(list, numberOfParts), ORDERED), false);
    }

    @Override
    public boolean hasNext() {
        return i < size && chunks != 0;
    }

    @Override
    public List<T> next() {
        List<T> batch = source.subList(i, i + chunkSize);
        i = i + chunkSize;
        leftElements = leftElements - chunkSize;
        chunkSize = (int) Math.ceil(((double) leftElements) / --chunks);
        return batch;
    }
}
