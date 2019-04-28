package com.pivovarit.collectors;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

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
        super(toEntry(keyMapper, valueMapper), toMapStrategy(duplicateKeyResolutionStrategy, mapFactory), executor, parallelism);
    }

    AsyncParallelMapCollector(
      Function<T, K> keyMapper,
      Function<T, V> valueMapper,
      BinaryOperator<V> duplicateKeyResolutionStrategy,
      Supplier<M> mapFactory,
      Executor executor) {
        super(toEntry(keyMapper, valueMapper), toMapStrategy(duplicateKeyResolutionStrategy, mapFactory), executor);
    }
}
