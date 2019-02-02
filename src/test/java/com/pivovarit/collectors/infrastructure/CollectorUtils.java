package com.pivovarit.collectors.infrastructure;

import com.pivovarit.collectors.ParallelCollectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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

    public static <T, R> List<TriFunction<Function<T, R>, Executor, Integer, Collector<T, List<CompletableFuture<R>>, ? extends CompletableFuture<? extends Collection<R>>>>> getCollectorsAsLamdasWithMapper() {
        ArrayList<
                TriFunction<
                        Function<T, R>,
                        Executor,
                        Integer,
                        Collector<T, List<CompletableFuture<R>>, ? extends CompletableFuture<? extends Collection<R>>>
                        >
                > collectors = new ArrayList<>(3);

        collectors.add(ParallelCollectors::parallelToList);
        collectors.add(ParallelCollectors::parallelToSet);
        collectors.add((mapper, executor, parallelism) -> ParallelCollectors.parallelToCollection(mapper, ArrayList::new, executor, parallelism));

        return collectors;
    }

    public static <T> List<BiFunction<Executor, Integer, Collector<Supplier<T>, List<CompletableFuture<T>>, ? extends CompletableFuture<? extends Collection<T>>>>> getCollectorsAsLamdas() {
        ArrayList<
                BiFunction<
                        Executor,
                        Integer,
                        Collector<Supplier<T>, List<CompletableFuture<T>>, ? extends CompletableFuture<? extends Collection<T>>>
                        >
                > collectors = new ArrayList<>(3);

        collectors.add(ParallelCollectors::parallelToList);
        collectors.add(ParallelCollectors::parallelToSet);
        collectors.add((executor, parallelism) -> ParallelCollectors.parallelToCollection(ArrayList::new, executor, parallelism));

        return collectors;
    }

}
