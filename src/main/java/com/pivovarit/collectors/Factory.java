/*
 * Copyright 2014-2026 Grzegorz Piwowarek, https://4comprehension.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pivovarit.collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class Factory {

    private Factory() {
    }

    static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> async(
      Function<? super T, ? extends R> mapper,
      Config config) {
        if (config.batching()) {
            return asyncBatching(mapper, config);
        }
        Executor executor = ConfigProcessor.resolveExecutor(config);
        return Collector.of(
          () -> newAsyncContainer(executor, config),
          (AsyncContainer<R> c, T t) -> c.futures.add(c.dispatcher.enqueue(() -> mapper.apply(t))),
          Factory::uoeCombiner,
          c -> asyncFinish(c, futures -> futures.stream().map(CompletableFuture::join))
        );
    }

    static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> async(
      Function<? super T, ? extends R> mapper,
      Config config,
      Collector<R, ?, RR> downstream) {
        if (config.batching()) {
            return asyncBatching(mapper, config, downstream);
        }
        Executor executor = ConfigProcessor.resolveExecutor(config);
        return Collector.of(
          () -> newAsyncContainer(executor, config),
          (AsyncContainer<R> c, T t) -> c.futures.add(c.dispatcher.enqueue(() -> mapper.apply(t))),
          Factory::uoeCombiner,
          c -> asyncFinish(c, futures -> futures.stream().map(CompletableFuture::join).collect(downstream))
        );
    }

    static <T, R> Collector<T, ?, Stream<R>> streaming(
      Function<? super T, ? extends R> mapper,
      Config config) {
        if (isSync(config)) {
            return sync(mapper);
        }
        if (config.batching()) {
            return streamingBatching(mapper, config);
        }
        Executor executor = ConfigProcessor.resolveExecutor(config);
        return Collector.of(
          () -> newAsyncContainer(executor, config),
          (AsyncContainer<R> c, T t) -> c.futures.add(c.dispatcher.enqueue(() -> mapper.apply(t))),
          Factory::uoeCombiner,
          c -> {
              c.dispatcher.close();
              if (config.ordered()) {
                  return orderedStream(c.futures);
              }
              return StreamSupport.stream(new CompletionOrderSpliterator<>(c.futures), false);
          }
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T, K, R> Collector<T, ?, CompletableFuture<Stream<Group<K, R>>>> asyncGrouping(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Config config) {
        Executor executor = ConfigProcessor.resolveExecutor(config);
        Function<T, R> narrowMapper = (Function) mapper;
        Function<List<Group<K, R>>, Stream<Group<K, R>>> reducer = List::stream;
        return Collector.of(
          (java.util.function.Supplier<GroupingContainer<K, T>>) GroupingContainer::new,
          (GroupingContainer<K, T> c, T t) -> c.groups.computeIfAbsent(classifier.apply(t), k -> new ArrayList<>()).add(t),
          Factory::uoeCombiner,
          c -> groupingAsyncFinish(c, executor, config, narrowMapper, reducer)
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> asyncGrouping(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Config config,
      Collector<Group<K, R>, ?, RR> downstream) {
        Executor executor = ConfigProcessor.resolveExecutor(config);
        Function<T, R> narrowMapper = (Function) mapper;
        Function<List<Group<K, R>>, RR> reducer = groups -> groups.stream().collect(downstream);
        return Collector.of(
          (java.util.function.Supplier<GroupingContainer<K, T>>) GroupingContainer::new,
          (GroupingContainer<K, T> c, T t) -> c.groups.computeIfAbsent(classifier.apply(t), k -> new ArrayList<>()).add(t),
          Factory::uoeCombiner,
          c -> groupingAsyncFinish(c, executor, config, narrowMapper, reducer)
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T, K, R> Collector<T, ?, Stream<Group<K, R>>> streamingGrouping(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper,
      Config config) {
        if (isSync(config)) {
            return syncGrouping(classifier, mapper);
        }
        Executor executor = ConfigProcessor.resolveExecutor(config);
        Function<T, R> narrowMapper = (Function) mapper;
        return Collector.of(
          (java.util.function.Supplier<GroupingContainer<K, T>>) GroupingContainer::new,
          (GroupingContainer<K, T> c, T t) -> c.groups.computeIfAbsent(classifier.apply(t), k -> new ArrayList<>()).add(t),
          Factory::uoeCombiner,
          c -> {
              List<CompletableFuture<Group<K, R>>> futures = Factory.<K, T, R>submitGroups(c.groups, executor, config, narrowMapper);
              if (config.ordered()) {
                  return orderedStream(futures);
              }
              return StreamSupport.stream(new CompletionOrderSpliterator<>(futures), false);
          }
        );
    }

    static <T, R> Collector<T, ?, Stream<R>> sync(Function<? super T, ? extends R> mapper) {
        return Collector.of(
          ArrayList<T>::new,
          List::add,
          Factory::uoeCombiner,
          list -> list.stream().map(mapper)
        );
    }

    static <T, K, R> Collector<T, ?, Stream<Group<K, R>>> syncGrouping(
      Function<? super T, ? extends K> classifier,
      Function<? super T, ? extends R> mapper) {
        return Collector.of(
          (java.util.function.Supplier<GroupingContainer<K, T>>) GroupingContainer::new,
          (GroupingContainer<K, T> c, T t) -> c.groups.computeIfAbsent(classifier.apply(t), k -> new ArrayList<>()).add(t),
          Factory::uoeCombiner,
          c -> c.groups.entrySet().stream()
            .map(e -> new Group<>(e.getKey(), e.getValue().stream().<R>map(mapper).toList()))
        );
    }

    private static <T, R> Collector<T, ?, CompletableFuture<Stream<R>>> asyncBatching(
      Function<? super T, ? extends R> mapper,
      Config config) {
        Executor executor = ConfigProcessor.resolveExecutor(config);
        int parallelism = config.parallelism();
        return Collector.of(
          ArrayList<T>::new,
          List::add,
          Factory::uoeCombiner,
          items -> {
              if (items.isEmpty()) {
                  return CompletableFuture.completedFuture(Stream.<R>empty());
              }
              BatchContext ctx = new BatchContext();
              List<CompletableFuture<List<R>>> futures = submitBatches(items, parallelism, executor, config, mapper, ctx);
              CompletableFuture<Stream<R>> result = new CompletableFuture<>();
              for (CompletableFuture<List<R>> f : futures) {
                  f.whenComplete((v, e) -> {
                      if (e != null && !result.isDone()) {
                          Throwable primary = ctx.primaryException();
                          result.completeExceptionally(primary != null ? primary : unwrap(e));
                      }
                  });
              }
              CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, e) -> {
                    if (e != null) {
                        if (!result.isDone()) {
                            Throwable primary = ctx.primaryException();
                            result.completeExceptionally(primary != null ? primary : unwrap(e));
                        }
                    } else {
                        result.complete(futures.stream().flatMap(f -> f.join().stream()));
                    }
                });
              return result;
          }
        );
    }

    private static <T, R, RR> Collector<T, ?, CompletableFuture<RR>> asyncBatching(
      Function<? super T, ? extends R> mapper,
      Config config,
      Collector<R, ?, RR> downstream) {
        Executor executor = ConfigProcessor.resolveExecutor(config);
        int parallelism = config.parallelism();
        return Collector.of(
          ArrayList<T>::new,
          List::add,
          Factory::uoeCombiner,
          items -> {
              if (items.isEmpty()) {
                  return CompletableFuture.completedFuture(Stream.<R>empty().collect(downstream));
              }
              BatchContext ctx = new BatchContext();
              List<CompletableFuture<List<R>>> futures = submitBatches(items, parallelism, executor, config, mapper, ctx);
              CompletableFuture<RR> result = new CompletableFuture<>();
              for (CompletableFuture<List<R>> f : futures) {
                  f.whenComplete((v, e) -> {
                      if (e != null && !result.isDone()) {
                          Throwable primary = ctx.primaryException();
                          result.completeExceptionally(primary != null ? primary : unwrap(e));
                      }
                  });
              }
              CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, e) -> {
                    if (e != null) {
                        if (!result.isDone()) {
                            Throwable primary = ctx.primaryException();
                            result.completeExceptionally(primary != null ? primary : unwrap(e));
                        }
                    } else {
                        try {
                            result.complete(futures.stream()
                              .flatMap(f -> f.join().stream())
                              .collect(downstream));
                        } catch (Throwable t) {
                            result.completeExceptionally(t);
                        }
                    }
                });
              return result;
          }
        );
    }

    private static <T, R> Collector<T, ?, Stream<R>> streamingBatching(
      Function<? super T, ? extends R> mapper,
      Config config) {
        Executor executor = ConfigProcessor.resolveExecutor(config);
        int parallelism = config.parallelism();
        return Collector.of(
          ArrayList<T>::new,
          List::add,
          Factory::uoeCombiner,
          items -> {
              if (items.isEmpty()) {
                  return Stream.<R>empty();
              }
              BatchContext ctx = new BatchContext();
              List<CompletableFuture<List<R>>> futures = submitBatches(items, parallelism, executor, config, mapper, ctx);
              if (config.ordered()) {
                  return orderedStream(futures).flatMap(Collection::stream);
              }
              return StreamSupport.stream(new CompletionOrderSpliterator<>(futures), false)
                .flatMap(Collection::stream);
          }
        );
    }

    private static <T, R> List<CompletableFuture<List<R>>> submitBatches(
      List<T> items,
      int parallelism,
      Executor executor,
      Config config,
      Function<? super T, ? extends R> mapper,
      BatchContext context) {
        List<List<T>> batches = BatchingSpliterator.partition(items, parallelism);
        List<CompletableFuture<List<R>>> futures = new ArrayList<>(batches.size());
        for (List<T> batch : batches) {
            CompletableFuture<List<R>> cf = new CompletableFuture<>();
            Runnable body = () -> {
                Thread self = Thread.currentThread();
                context.running.add(self);
                try {
                    if (context.failed.get()) {
                        cf.completeExceptionally(new java.util.concurrent.CancellationException());
                        return;
                    }
                    List<R> out = new ArrayList<>(batch.size());
                    for (T t : batch) {
                        if (context.failed.get()) {
                            cf.completeExceptionally(new java.util.concurrent.CancellationException());
                            return;
                        }
                        out.add(mapper.apply(t));
                    }
                    cf.complete(out);
                } catch (Throwable e) {
                    cf.completeExceptionally(e);
                    context.onFailure(e);
                } finally {
                    context.running.remove(self);
                }
            };
            if (config.taskDecorator() != null) {
                body = config.taskDecorator().apply(body);
            }
            try {
                executor.execute(body);
            } catch (Throwable t) {
                cf.completeExceptionally(t);
                context.onFailure(t);
            }
            futures.add(cf);
        }
        return futures;
    }

    private static <K, T, R> List<CompletableFuture<Group<K, R>>> submitGroups(
      Map<K, List<T>> groups,
      Executor executor,
      Config config,
      Function<? super T, ? extends R> mapper) {
        Dispatcher dispatcher = new Dispatcher(executor, config.parallelism(), config.taskDecorator());
        dispatcher.start();
        List<CompletableFuture<Group<K, R>>> futures = new ArrayList<>(groups.size());
        for (Map.Entry<K, List<T>> entry : groups.entrySet()) {
            K key = entry.getKey();
            List<T> items = entry.getValue();
            CompletableFuture<Group<K, R>> cf = dispatcher.enqueue(() -> {
                List<R> out = new ArrayList<>(items.size());
                for (T t : items) {
                    out.add(mapper.apply(t));
                }
                return new Group<>(key, out);
            });
            futures.add(cf);
        }
        dispatcher.close();
        return futures;
    }

    private static <K, T, R, X> CompletableFuture<X> groupingAsyncFinish(
      GroupingContainer<K, T> c,
      Executor executor,
      Config config,
      Function<? super T, ? extends R> mapper,
      Function<List<Group<K, R>>, X> reducer) {
        if (c.groups.isEmpty()) {
            return CompletableFuture.completedFuture(reducer.apply(List.of()));
        }
        Dispatcher dispatcher = new Dispatcher(executor, config.parallelism(), config.taskDecorator());
        dispatcher.start();
        List<CompletableFuture<Group<K, R>>> futures = new ArrayList<>(c.groups.size());
        for (Map.Entry<K, List<T>> entry : c.groups.entrySet()) {
            K key = entry.getKey();
            List<T> items = entry.getValue();
            CompletableFuture<Group<K, R>> cf = dispatcher.enqueue(() -> {
                List<R> out = new ArrayList<>(items.size());
                for (T t : items) {
                    out.add(mapper.apply(t));
                }
                return new Group<>(key, out);
            });
            futures.add(cf);
        }
        dispatcher.close();

        CompletableFuture<X> result = new CompletableFuture<>();
        for (CompletableFuture<Group<K, R>> f : futures) {
            f.whenComplete((v, e) -> {
                if (e != null && !result.isDone()) {
                    Throwable primary = dispatcher.primaryException();
                    result.completeExceptionally(primary != null ? primary : unwrap(e));
                }
            });
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .whenComplete((v, e) -> {
              if (e != null) {
                  if (!result.isDone()) {
                      Throwable primary = dispatcher.primaryException();
                      result.completeExceptionally(primary != null ? primary : unwrap(e));
                  }
              } else {
                  try {
                      List<Group<K, R>> groups = new ArrayList<>(futures.size());
                      for (CompletableFuture<Group<K, R>> f : futures) {
                          groups.add(f.join());
                      }
                      result.complete(reducer.apply(groups));
                  } catch (Throwable t) {
                      result.completeExceptionally(t);
                  }
              }
          });
        return result;
    }

    private static <R> AsyncContainer<R> newAsyncContainer(Executor executor, Config config) {
        Dispatcher dispatcher = new Dispatcher(executor, config.parallelism(), config.taskDecorator());
        dispatcher.start();
        return new AsyncContainer<>(dispatcher);
    }

    private static <R, X> CompletableFuture<X> asyncFinish(
      AsyncContainer<R> c,
      Function<List<CompletableFuture<R>>, X> reducer) {
        c.dispatcher.close();
        CompletableFuture<X> result = new CompletableFuture<>();
        for (CompletableFuture<R> f : c.futures) {
            f.whenComplete((v, e) -> {
                if (e != null && !result.isDone()) {
                    Throwable primary = c.dispatcher.primaryException();
                    result.completeExceptionally(primary != null ? primary : unwrap(e));
                }
            });
        }
        CompletableFuture.allOf(c.futures.toArray(new CompletableFuture[0]))
          .whenComplete((v, e) -> {
              if (e != null) {
                  if (!result.isDone()) {
                      Throwable primary = c.dispatcher.primaryException();
                      result.completeExceptionally(primary != null ? primary : unwrap(e));
                  }
              } else {
                  try {
                      result.complete(reducer.apply(c.futures));
                  } catch (Throwable t) {
                      result.completeExceptionally(t);
                  }
              }
          });
        return result;
    }

    private static Throwable unwrap(Throwable e) {
        if (e instanceof CompletionException && e.getCause() != null) {
            return e.getCause();
        }
        return e;
    }

    private static <R> Stream<R> orderedStream(List<CompletableFuture<R>> futures) {
        return futures.stream().flatMap(f -> {
            try {
                return Stream.of(f.join());
            } catch (java.util.concurrent.CancellationException ce) {
                return Stream.empty();
            } catch (CompletionException ce) {
                if (ce.getCause() instanceof java.util.concurrent.CancellationException) {
                    return Stream.empty();
                }
                throw ce;
            }
        });
    }

    private static boolean isSync(Config config) {
        return config.parallelism() != null && config.parallelism() == 1;
    }

    private static <A> A uoeCombiner(A a, A b) {
        throw new UnsupportedOperationException();
    }

    static final class AsyncContainer<R> {
        final Dispatcher dispatcher;
        final List<CompletableFuture<R>> futures = new ArrayList<>();

        AsyncContainer(Dispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }
    }

    static final class GroupingContainer<K, T> {
        final LinkedHashMap<K, List<T>> groups = new LinkedHashMap<>();
    }

    static final class BatchContext {
        final java.util.Set<Thread> running = java.util.concurrent.ConcurrentHashMap.newKeySet();
        final java.util.concurrent.atomic.AtomicBoolean failed = new java.util.concurrent.atomic.AtomicBoolean();
        final java.util.concurrent.atomic.AtomicReference<Throwable> primary = new java.util.concurrent.atomic.AtomicReference<>();

        void onFailure(Throwable cause) {
            if (failed.compareAndSet(false, true)) {
                primary.set(cause);
                for (Thread t : running) {
                    t.interrupt();
                }
            }
        }

        Throwable primaryException() {
            return primary.get();
        }
    }
}
