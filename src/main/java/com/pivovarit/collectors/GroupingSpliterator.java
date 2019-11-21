package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

class GroupingSpliterator<T> implements Spliterator<List<T>> {

    private final Iterator<T> source;
    private final int maxPartitionSize;
    private final List<T> accumulator;
    private final int size;

    private GroupingSpliterator(Collection<T> source, int partitions) {
        this.source = source.iterator();
        this.maxPartitionSize = (int) Math.ceil((double) source.size() / (double) partitions);
        this.accumulator = new ArrayList<>(maxPartitionSize);
        this.size = partitions;
    }

    public static <T> Spliterator<List<T>> partitioned(Collection<T> source, int partitions) {
        return new GroupingSpliterator<>(source, partitions);
    }

    @Override
    public boolean tryAdvance(Consumer<? super List<T>> action) {
        if (!source.hasNext()) return false;

        while (source.hasNext() && accumulator.size() < maxPartitionSize) {
            accumulator.add(source.next());
        }

        action.accept(new ArrayList<>(accumulator));
        accumulator.clear();
        return true;
    }

    @Override
    public Spliterator<List<T>> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return size;
    }

    @Override
    public int characteristics() {
        return 0;
    }
}
