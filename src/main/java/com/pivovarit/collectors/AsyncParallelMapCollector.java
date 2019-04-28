package com.pivovarit.collectors;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncParallelMapCollector<T, K, V, M extends Map<K, V>>
  extends AsyncParallelCollector<T, Entry<K, V>, M> {

    AsyncParallelMapCollector(
      Function<T, K> keyMapper,
      Function<T, V> valueMapper,
      BinaryOperator<V> duplicateKeyResolutionStrategy,
      Supplier<M> mapFactory,
      Executor executor,
      int parallelism) {
        super(toEntry(keyMapper, valueMapper), postProcess(duplicateKeyResolutionStrategy, mapFactory), executor, parallelism);
    }

    AsyncParallelMapCollector(
      Function<T, K> keyMapper,
      Function<T, V> valueMapper,
      BinaryOperator<V> duplicateKeyResolutionStrategy,
      Supplier<M> mapFactory,
      Executor executor) {
        super(toEntry(keyMapper, valueMapper), postProcess(duplicateKeyResolutionStrategy, mapFactory), executor);
    }

    private static <K, V, M extends Map<K, V>>Function<CompletableFuture<Stream<Entry<K, V>>>, CompletableFuture<M>> postProcess(BinaryOperator<V> duplicateKeyResolutionStrategy, Supplier<M> mapFactory) {
        return result -> result.thenApply(futures -> futures.collect(toMap(Entry::getKey, Entry::getValue, duplicateKeyResolutionStrategy, mapFactory)));
    }

    private static <T, K, V> Function<T, Entry<K, V>> toEntry(Function<T, K> keyMapper, Function<T, V> valueMapper) {
        return entry -> new AbstractMap.SimpleEntry<>(keyMapper.apply(entry), valueMapper.apply(entry));
    }
}
