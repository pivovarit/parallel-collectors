package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * @author Grzegorz Piwowarek
 */
abstract class AbstractOrderedParallelCollector<T, R, C>
  extends AbstractParallelCollector<T, Map.Entry<Integer, R>, C> {

    static <R, C extends Collection<R>> Function<List<CompletableFuture<Map.Entry<Integer, R>>>, CompletableFuture<C>> foldLeftFuturesOrdered(Supplier<C> collectionFactory) {
        return futures -> futures.stream()
          .reduce(completedFuture(new ArrayList<>()),
            accumulatingResults(),
            mergingPartialResults())
          .thenApply(list -> list.stream()
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .map(Map.Entry::getValue)
            .collect(Collectors.toCollection(collectionFactory)));
    }
}
