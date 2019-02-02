package com.pivovarit.collectors.infrastructure;

import com.pivovarit.collectors.ParallelCollectors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author Robert Piwowarek
 */
public final class CollectorUtils {
    private CollectorUtils() {
    }

    public static <T> List<BiFunction<ThreadPoolExecutor, Integer, Collector<Supplier<T>, List<CompletableFuture<T>>, ? extends CompletableFuture<? extends Collection<T>>>>> getCollectorsAsLamdas() {
        var collectors = new ArrayList<BiFunction<ThreadPoolExecutor, Integer, Collector<Supplier<T>, List<CompletableFuture<T>>, ? extends CompletableFuture<? extends Collection<T>>>>>(3);

        collectors.add(ParallelCollectors::parallelToList);
        collectors.add(ParallelCollectors::parallelToSet);
        collectors.add((executor, parallelism) -> ParallelCollectors.parallelToCollection(ArrayList::new, executor, parallelism));

        return collectors;
    }

    public static <T, R> List<Collector> getCollectors(Function<T, R> mapper, ThreadPoolExecutor executor, Integer parallelism) {
        var collectors = new ArrayList<Collector>(3);
        collectors.add(ParallelCollectors.parallelToList(mapper, executor, parallelism));
        collectors.add(ParallelCollectors.parallelToSet(mapper, executor, parallelism));
        return collectors;
    }

    public static List<Collector> getCollectors(ThreadPoolExecutor executor, Integer parallelism) {
        var collectors = new ArrayList<Collector>(3);
        collectors.add(ParallelCollectors.parallelToList(executor, parallelism));
        collectors.add(ParallelCollectors.parallelToSet(executor, parallelism));
        return collectors;
    }

    public static List<Collector> getCollectors(ThreadPoolExecutor executor) {
        var collectors = new ArrayList<Collector>(3);
        collectors.add(ParallelCollectors.parallelToList(executor));
        collectors.add(ParallelCollectors.parallelToSet(executor));
        return collectors;
    }
}
