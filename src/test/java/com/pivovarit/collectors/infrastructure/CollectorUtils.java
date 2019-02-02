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

    public static <T, R> List<TriFunction<Function<T, R>, ThreadPoolExecutor, Integer, Collector<T, List<CompletableFuture<R>>, ? extends CompletableFuture<? extends Collection<R>>>>> getCollectorsAsLamdasWithMapper3() {
        var collectors = new ArrayList<TriFunction<Function<T, R>, ThreadPoolExecutor, Integer, Collector<T, List<CompletableFuture<R>>, ? extends CompletableFuture<? extends Collection<R>>>>>(3);

        collectors.add(ParallelCollectors::parallelToList);
        collectors.add(ParallelCollectors::parallelToSet);
        collectors.add((mapper, executor, parallelism) -> ParallelCollectors.parallelToCollection(mapper, ArrayList::new, executor, parallelism));

        return collectors;
    }

    public static <T, R> List<BiFunction<Function<T, R>, ThreadPoolExecutor, Collector<T, List<CompletableFuture<R>>, ? extends CompletableFuture<? extends Collection<R>>>>> getCollectorsAsLamdasWithMapper2() {
        var collectors = new ArrayList<BiFunction<Function<T, R>, ThreadPoolExecutor, Collector<T, List<CompletableFuture<R>>, ? extends CompletableFuture<? extends Collection<R>>>>>(3);

        collectors.add(ParallelCollectors::parallelToList);
        collectors.add(ParallelCollectors::parallelToSet);
        collectors.add((mapper, executor) -> ParallelCollectors.parallelToCollection(mapper, ArrayList::new, executor));

        return collectors;
    }

    public static <T> List<BiFunction<ThreadPoolExecutor, Integer, Collector<Supplier<T>, List<CompletableFuture<T>>, ? extends CompletableFuture<? extends Collection<T>>>>> getCollectorsAsLamdas2() {
        var collectors = new ArrayList<BiFunction<ThreadPoolExecutor, Integer, Collector<Supplier<T>, List<CompletableFuture<T>>, ? extends CompletableFuture<? extends Collection<T>>>>>(3);

        collectors.add(ParallelCollectors::parallelToList);
        collectors.add(ParallelCollectors::parallelToSet);
        collectors.add((executor, parallelism) -> ParallelCollectors.parallelToCollection(ArrayList::new, executor, parallelism));

        return collectors;
    }

    public static <T> List<Function<ThreadPoolExecutor, Collector<Supplier<T>, List<CompletableFuture<T>>, ? extends CompletableFuture<? extends Collection<T>>>>> getCollectorsAsLamdas1() {
        var collectors = new ArrayList<Function<ThreadPoolExecutor, Collector<Supplier<T>, List<CompletableFuture<T>>, ? extends CompletableFuture<? extends Collection<T>>>>>(3);

        collectors.add(ParallelCollectors::parallelToList);
        collectors.add(ParallelCollectors::parallelToSet);
        collectors.add((executor) -> ParallelCollectors.parallelToCollection(ArrayList::new, executor));

        return collectors;
    }

}
