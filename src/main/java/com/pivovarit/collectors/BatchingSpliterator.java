package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static java.util.stream.StreamSupport.stream;

/**
 * @author Grzegorz Piwowarek
 */
final class BatchingSpliterator<T> implements Spliterator<List<T>> {

    private List<T> source;
    private int maxChunks;

    private int chunks;
    private int chunkSize;
    private int consumed;

    private BatchingSpliterator(List<T> list, int batches) {
        if (batches < 1) {
            throw new IllegalArgumentException("batches can't be lower than one");
        }
        source = list;
        chunks = batches;
        maxChunks = Math.min(list.size(), batches);
        chunkSize = (int) Math.ceil(((double) source.size()) / batches);
    }

    static <T> Stream<List<T>> partitioned(List<T> list, int numberOfParts) {
        int size = list.size();

        if (size <= numberOfParts) {
            return asSingletonListStream(list);
        } else if (size == 0) {
            return empty();
        } else if (numberOfParts == 1) {
            return of(list);
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
        if (consumed < source.size() && chunks != 0) {
            List<T> batch = source.subList(consumed, consumed + chunkSize);
            consumed += chunkSize;
            chunkSize = (int) Math.ceil(((double) (source.size() - consumed)) / --chunks);
            action.accept(batch);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Spliterator<List<T>> trySplit() {
        if (actualBatchCount(source, chunks) > 1 || consumed == 0 ) {
            var first = source.subList(0, source.size() / 2);
            var second = source.subList(source.size() / 2, source.size());
            var originalChunks = chunks;

            source = first;
            chunks = originalChunks % 2 == 0 ? originalChunks / 2 : originalChunks / 2 + 1;
            maxChunks = Math.min(source.size(), chunks);
            chunkSize = (int) Math.ceil(((double) source.size()) / chunks);
            return new BatchingSpliterator<>(second, originalChunks / 2);
        }
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

    private static <T> int actualBatchCount(List<T> list, int numberOfBatches) {
        int batchSize = list.size() / numberOfBatches;
        int remainder = list.size() % numberOfBatches;

        int batches = 0;
        int currentIndex = 0;

        for (int i = 0; i < numberOfBatches; i++) {
            int currentBatchSize = batchSize + (remainder > 0 ? 1 : 0);
            remainder--;

            int nextIndex = Math.min(currentIndex + currentBatchSize, list.size());
            if (currentIndex < nextIndex) {
                batches++;
            }
            currentIndex = nextIndex;
        }

        return batches;
    }
}
