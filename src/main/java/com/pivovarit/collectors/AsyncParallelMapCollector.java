package com.pivovarit.collectors;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptySet;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncParallelMapCollector<T, K, V, M extends Map<K, V>>
  extends AbstractAsyncCollector<T, Entry<K, V>, M>
  implements AutoCloseable {

    private final Dispatcher<Entry<K, V>> dispatcher;

    private final Function<T, K> keyMapper;
    private final Function<T, V> valueMapper;
    private final BinaryOperator<V> duplicateKeyResolutionStrategy;
    private final Supplier<M> mapFactory;

    AsyncParallelMapCollector(
      Function<T, K> keyMapper,
      Function<T, V> valueMapper,
      BinaryOperator<V> duplicateKeyResolutionStrategy,
      Supplier<M> mapFactory,
      Executor executor,
      int parallelism) {
        this.dispatcher = new ThrottlingDispatcher<>(executor, parallelism);
        this.keyMapper = keyMapper;
        this.valueMapper = valueMapper;
        this.duplicateKeyResolutionStrategy = duplicateKeyResolutionStrategy;
        this.mapFactory = mapFactory;
    }

    AsyncParallelMapCollector(
      Function<T, K> keyMapper,
      Function<T, V> valueMapper,
      BinaryOperator<V> duplicateKeyResolutionStrategy,
      Supplier<M> mapFactory,
      Executor executor) {
        this.dispatcher = new UnboundedDispatcher<>(executor);
        this.keyMapper = keyMapper;
        this.valueMapper = valueMapper;
        this.duplicateKeyResolutionStrategy = duplicateKeyResolutionStrategy;
        this.mapFactory = mapFactory;
    }

    @Override
    public BiConsumer<List<CompletableFuture<Entry<K, V>>>, T> accumulator() {
        return (acc, e) -> acc.add(dispatcher.enqueue(
          () -> new AbstractMap.SimpleEntry<>(
            keyMapper.apply(e),
            valueMapper.apply(e))));
    }

    @Override
    public Function<List<CompletableFuture<Entry<K, V>>>, CompletableFuture<M>> finisher() {
        if (!dispatcher.isEmpty()) {
            dispatcher.start();
            return futures -> CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
              .thenApply(__ -> futures.stream()
                .map(CompletableFuture::join)
                .collect(collectingAndThen(toMap(Entry::getKey, Entry::getValue, duplicateKeyResolutionStrategy, mapFactory),
                  f -> {
                      try {
                          return f;
                      } finally {
                          dispatcher.close();
                      }
                  })));
        } else {
            return supplyWithResources(() -> (__) -> completedFuture(mapFactory.get()), dispatcher::close);
        }
    }

    @Override
    public Set<Characteristics> characteristics() {
        return emptySet();
    }

    @Override
    public void close() {
        dispatcher.close();
    }

    private static <R, C extends Collection<R>> Function<List<CompletableFuture<Entry<Integer, R>>>, CompletableFuture<C>> foldLeftFuturesOrdered(Supplier<C> collectionFactory) {
        return futures -> allOf(futures.toArray(new CompletableFuture<?>[0]))
          .thenApply(__ -> futures.stream()
            .map(CompletableFuture::join)
            .sorted(Comparator.comparing(Entry::getKey))
            .map(Entry::getValue)
            .collect(toCollection(collectionFactory)));
    }
}
