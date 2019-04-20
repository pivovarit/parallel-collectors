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
  extends AbstractAsyncParallelCollector<T, Entry<K, V>, M> {

    private final BinaryOperator<V> duplicateKeyResolutionStrategy;
    private final Supplier<M> mapFactory;

    AsyncParallelMapCollector(
      Function<T, K> keyMapper,
      Function<T, V> valueMapper,
      BinaryOperator<V> duplicateKeyResolutionStrategy,
      Supplier<M> mapFactory,
      Executor executor,
      int parallelism) {
        super(toEntry(keyMapper, valueMapper), executor, parallelism);
        this.duplicateKeyResolutionStrategy = duplicateKeyResolutionStrategy;
        this.mapFactory = mapFactory;
    }

    AsyncParallelMapCollector(
      Function<T, K> keyMapper,
      Function<T, V> valueMapper,
      BinaryOperator<V> duplicateKeyResolutionStrategy,
      Supplier<M> mapFactory,
      Executor executor) {
        super(toEntry(keyMapper, valueMapper), executor);
        this.duplicateKeyResolutionStrategy = duplicateKeyResolutionStrategy;
        this.mapFactory = mapFactory;
    }

    @Override
    Function<CompletableFuture<Stream<Entry<K, V>>>, CompletableFuture<M>> postProcess() {
        return result -> result.thenApply(futures -> futures.collect(toMap(Entry::getKey, Entry::getValue, duplicateKeyResolutionStrategy, mapFactory)));
    }

    private static <T, K, V> Function<T, Entry<K, V>> toEntry(Function<T, K> keyMapper, Function<T, V> valueMapper) {
        return entry -> new AbstractMap.SimpleEntry<>(keyMapper.apply(entry), valueMapper.apply(entry));
    }
}
