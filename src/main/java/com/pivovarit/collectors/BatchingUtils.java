package com.pivovarit.collectors;

import java.util.List;
import java.util.stream.Stream;

final class BatchingUtils {
    private BatchingUtils() {
    }

    static <T> Stream<List<T>> partitioned(List<T> list, int numberOfParts) {
        Stream.Builder<List<T>> builder = Stream.builder();
        int size = list.size();
        int chunkSize = (int) Math.ceil(((double) size) / numberOfParts);
        int leftElements = size;
        int i = 0;
        while (i < size && numberOfParts != 0) {
            builder.add(list.subList(i, i + chunkSize));
            i = i + chunkSize;
            leftElements = leftElements - chunkSize;
            chunkSize = (int) Math.ceil(((double) leftElements) / --numberOfParts);
        }
        return builder.build();
    }
}
