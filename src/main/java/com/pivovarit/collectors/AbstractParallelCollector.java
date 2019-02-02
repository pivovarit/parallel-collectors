package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.Collections.synchronizedList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * @author Grzegorz Piwowarek
 */
@SuppressWarnings("WeakerAccess")
abstract class AbstractParallelCollector<T, R, C extends Collection<R>>
  implements Collector<T, List<CompletableFuture<R>>, CompletableFuture<C>> {

    protected final Executor executor;

    protected final ExecutorService dispatcher = newSingleThreadExecutor(new CustomThreadFactory());

    protected final Queue<Supplier<R>> workingQueue;
    protected final Queue<CompletableFuture<R>> pending;

    protected final Function<T, R> operation;

    private final Supplier<C> collectionFactory;

    AbstractParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor) {
        this(operation, collection, executor, new ConcurrentLinkedQueue<>(), new ConcurrentLinkedQueue<>());
    }

    AbstractParallelCollector(
      Function<T, R> operation,
      Supplier<C> collection,
      Executor executor,
      Queue<Supplier<R>> workingQueue,
      Queue<CompletableFuture<R>> pending) {
        this.executor = executor;
        this.collectionFactory = collection;
        this.operation = operation;
        this.workingQueue = workingQueue;
        this.pending = pending;
    }

    @Override
    abstract public BiConsumer<List<CompletableFuture<R>>, T> accumulator();

    @Override
    public abstract Set<Characteristics> characteristics();

    abstract protected Runnable dispatch(Queue<Supplier<R>> tasks);

    @Override
    public Supplier<List<CompletableFuture<R>>> supplier() {
        return () -> synchronizedList(new ArrayList<>());
    }

    @Override
    public BinaryOperator<List<CompletableFuture<R>>> combiner() {
        return (left, right) -> {
            left.addAll(right);
            return left;
        };
    }

    @Override
    public Function<List<CompletableFuture<R>>, CompletableFuture<C>> finisher() {
        if (workingQueue.size() != 0) {
            dispatcher.execute(dispatch(workingQueue));
            return foldLeftFutures().andThen(f -> tryWithResources(() -> f, dispatcher::shutdown));
        } else {
            return tryWithResources(this::foldLeftFutures, dispatcher::shutdown);
        }
    }

    private Function<List<CompletableFuture<R>>, CompletableFuture<C>> foldLeftFutures() {
        return futures -> futures.stream()
          .reduce(completedFuture(collectionFactory.get()),
            accumulatingResults(),
            mergingPartialResults());
    }

    static <T> T tryWithResources(Supplier<T> action, Runnable cleanup) {
        try {
            return action.get();
        } finally {
            cleanup.run();
        }
    }

    private static <T1, R1 extends Collection<T1>> BinaryOperator<CompletableFuture<R1>> mergingPartialResults() {
        return (f1, f2) -> f1.thenCombine(f2, (left, right) -> {
            left.addAll(right);
            return left;
        });
    }

    private static <T1, R1 extends Collection<T1>> BiFunction<CompletableFuture<R1>, CompletableFuture<T1>, CompletableFuture<R1>> accumulatingResults() {
        return (list, object) -> list.thenCombine(object, (left, right) -> {
            left.add(right);
            return left;
        });
    }
}
