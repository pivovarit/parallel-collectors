package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Runtime.getRuntime;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * @author Grzegorz Piwowarek
 */
final class AsyncParallelPartitioningCollector<T, R, C>
  implements Collector<List<T>, List<CompletableFuture<List<R>>>, CompletableFuture<C>> {

    private final Dispatcher<List<R>> dispatcher;
    private final Function<T, R> mapper;
    private final Function<CompletableFuture<Stream<List<R>>>, CompletableFuture<C>> processor;

    private final CompletableFuture<C> result = new CompletableFuture<>();

    AsyncParallelPartitioningCollector(
      Function<T, R> mapper,
      Function<CompletableFuture<Stream<List<R>>>, CompletableFuture<C>> processor,
      Executor executor) {
        this.dispatcher = Dispatcher.unbounded(executor);
        this.processor = processor;
        this.mapper = mapper;
    }

    @Override
    public Supplier<List<CompletableFuture<List<R>>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BinaryOperator<List<CompletableFuture<List<R>>>> combiner() {
        return (left, right) -> {
            left.addAll(right);
            return left;
        };
    }

    @Override
    public BiConsumer<List<CompletableFuture<List<R>>>, List<T>> accumulator() {
        return (acc, chunk) -> {
            startConsuming();
            acc.add(dispatcher.enqueue(() -> chunk.stream().map(mapper).collect(Collectors.toList())));
        };
    }

    @Override
    public Function<List<CompletableFuture<List<R>>>, CompletableFuture<C>> finisher() {
        return futures -> {
            dispatcher.stop();

            processor.apply(toCombined(futures))
              .whenComplete(processResult());

            return result;
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }

    private static <T> CompletableFuture<Stream<T>> toCombined(List<CompletableFuture<T>> futures) {
        return allOf(futures)
          .thenApply(__ -> futures.stream()
            .map(CompletableFuture::join));
    }

    private void startConsuming() {
        if (!dispatcher.isRunning()) {
            dispatcher.start()
              .exceptionally(throwable -> {
                  result.completeExceptionally(throwable);
                  return null;
              });
        }
    }

    private static <T> CompletableFuture<Void> allOf(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        futures.forEach(f -> f.handle((__, ex) -> ex != null && result.completeExceptionally(ex)));

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .handle((__, ex) -> ex != null ? result.completeExceptionally(ex) : result.complete(null));

        return result;
    }

    private BiConsumer<C, Throwable> processResult() {
        return (c, throwable) -> {
            if (throwable == null) {
                result.complete(c);
            } else {
                result.completeExceptionally(throwable);
            }
        };
    }

    public static <T> Stream<List<T>> partitioned(List<T> list, int numberOfParts) {
        Stream.Builder<List<T>> builder = Stream.builder();
        int size = list.size();
        int chunkSize = (int) Math.ceil(((double) size) / numberOfParts);
        int leftElements = size;
        int i = 0;
        while (i < size && numberOfParts != 0) {
            builder.add(list.subList(i, i + chunkSize));
            i = i + chunkSize;
            leftElements = leftElements - chunkSize;
            chunkSize = (int) Math.ceil(((double) leftElements) / --numberOfParts);
        }
        List<List<T>> collect = builder.build().collect(toList());
        System.out.println("chunk: " + collect.toString());
        return collect.stream();
    }

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collectingToStream(Function<T, R> mapper, Executor executor) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return collectingToStream(mapper, executor, getDefaultParallelism());
    }

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> collectingToStream(Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);
        return collectingAndThen(toList(), list -> partitioned(list, parallelism)
          .collect(new AsyncParallelPartitioningCollector<>(mapper, r -> r.thenApply(s -> s.flatMap(Collection::stream)), executor)));
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> collectingWithCollector(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor) {
        requireNonNull(collector, "collector can't be null");
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        return collectingWithCollector(collector, mapper, executor, getDefaultParallelism());
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> collectingWithCollector(Collector<R, ?, RR> collector, Function<T, R> mapper, Executor executor, int parallelism) {
        requireNonNull(collector, "collector can't be null");
        requireNonNull(executor, "executor can't be null");
        requireNonNull(mapper, "mapper can't be null");
        requireValidParallelism(parallelism);
        return collectingAndThen(toList(), list -> partitioned(list, parallelism)
          .collect(new AsyncParallelPartitioningCollector<>(mapper, r -> r.thenApply(s -> s.flatMap(Collection::stream).collect(collector)), executor)));
    }

    private static int getDefaultParallelism() {
        return Math.max(getRuntime().availableProcessors() - 1, 1);
    }

    static void requireValidParallelism(int parallelism) {
        if (parallelism < 1) {
            throw new IllegalArgumentException("Parallelism can't be lower than 1");
        }
    }
}

