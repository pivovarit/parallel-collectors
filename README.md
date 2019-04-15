# Parallel Collectors for Java 8 Stream API - overcoming limitations of standard Parallel Streams

[![Build Status](https://travis-ci.org/pivovarit/parallel-collectors.svg?branch=master)](https://travis-ci.org/pivovarit/parallel-collectors)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.pivovarit/parallel-collectors/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.pivovarit/parallel-collectors)
  <a href="https://twitter.com/intent/follow?screen_name=pivovarit">
        <img src="https://img.shields.io/twitter/follow/pivovarit.svg?style=social&logo=twitter"
            alt="follow on Twitter"></a>

Parallel Collectors is a toolkit easing parallel collection processing in Java using Stream API. 

    list.stream()
      .collect(parallelToList(i -> foo(i), executor, 2)).orTimeout(1000, MILLISECONDS)
      .thenAccept(System.out::println)
      .thenRun(() -> System.out.println("Finished!"));
      
They are:
- lightweight (yes, you could achieve the same with Project Reactor, but that's often a way too big tool for the job)
- configurable (it's possible to provide your own `Executor` and `parallelism`)
- non-blocking (no need to block the main thread while waiting for the result to arrive)
- non-invasive (they are just custom implementations of `Collector` interface, no magic inside)
- versatile (missing an API for your use case? just `parallelToStream` and continue with Stream API)
- powerful (combined power of Stream API and `CompletableFutures` allows to specify timeouts, compose with other `CompletableFuture`s, or just perform the whole processing asynchronously) 

## Rationale

Stream API is a great tool for processing collections, especially if you need to parallelize execution of CPU-intensive tasks, for example:

    public static void parallelSetAll(int[] array, IntUnaryOperator generator) {
        Objects.requireNonNull(generator);
        IntStream.range(0, array.length).parallel().forEach(i -> { array[i] = generator.applyAsInt(i); });
    }
    
**However, all tasks managed by parallel Streams are executed on a shared `ForkJoinPool` instance**. 
Unfortunately, it's not the best choice for running blocking operations (at least without `ManagedBlocker`) which could easily lead to the saturation of the common pool, and to serious performance degradation of everything that uses it as well.

For example:

    List<String> result = list.parallelStream()
      .map(i -> foo(i)) // runs implicitly on ForkJoinPool.commonPool()
      .collect(Collectors.toList());

That problem has been already solved and the solution is simple - one needs to create a separate thread pool and offload the common one from blocking operations... but there's a catch.

**Sadly, Streams can run parallel computations only on the common `ForkJoinPool`** which effectively restricts the applicability of them to CPU-bound jobs.

However, there's a trick that allows running parallel Streams in a custom FJP instance... but it's considered harmful:

> Note, however, that this technique of submitting a task to a fork-join pool to run the parallel stream in that pool is an implementation "trick" and is not guaranteed to work. Indeed, the threads or thread pool that is used for execution of parallel streams is unspecified. By default, the common fork-join pool is used, but in different environments, different thread pools might end up being used. 

Says [Stuart Marks on StackOverflow](https://stackoverflow.com/questions/28985704/parallel-stream-from-a-hashset-doesnt-run-in-parallel/29272776#29272776). 

Plus, that approach was seriously flawed before JDK-10 - if a `Stream` was targeted towards another pool, splitting would still need to adhere to the parallelism of the common pool, and not the one of the targeted pool [[JDK8190974]](https://bugs.openjdk.java.net/browse/JDK-8190974).

## Philosophy

Parallel Collectors are unopinionated by design so it's up to their users to use them responsibly, which involves things like:
- proper configuration of a provided `Executor` and its lifecycle management
- choosing the right parallelism level
- making sure that the tool is applied in the right context

Make sure to read API documentation before using these in production.

## Words of Caution

Even if this tool makes it easy to parallelize things, it doesn't always mean that you should. **Parallelism comes with a price which can be often higher than the starting point.** Threads are expensive to create, maintain and switch between, and you can only create a limited number of them.

Often, this library will turn out to be a wrong tool for the job, it's important to follow up on the root cause and double-check if parallelism is the way to go.

**It often turns out that the root cause can be addressed, for example, by using a simple JOIN statement, reorganizing your data... or even just by choosing a different API method.**

## Basic API

The main entrypoint to the libary is the `com.pivovarit.collectors.ParallelCollectors` class - which mirrors the `java.util.stream.Collectors` class and contains static factory methods returning `java.util.stream.Collector` implementations enhanced with parallel processing capabilities.

Since the library relies on a native `java.util.stream.Collector` mechanism, it was possible to achieve compatibility with Stream API without performing any intrusive surgery.


#### Available Collectors:

_parallelToList_:

- `CompletableFuture<List<R>> parallelToList(Function<T, R> mapper, Executor executor)`
- `CompletableFuture<List<R>> parallelToList(Function<T, R> mapper, Executor executor, int parallelism)`

_parallelToListOrdered_:

- `CompletableFuture<List<R>> parallelToListOrdered(Function<T, R> mapper, Executor executor)`
- `CompletableFuture<List<R>> parallelToListOrdered(Function<T, R> mapper, Executor executor, int parallelism)`
- ...

_parallelToSet_:

- `CompletableFuture<Set<R>> parallelToSet(Function<T, R> mapper, Executor executor)`
- `CompletableFuture<Set<R>> parallelToSet(Function<T, R> mapper, Executor executor, int parallelism)`

_parallelToMap_:

- `CompletableFuture<Map<K, V>> parallelToMap(Function<T, K> keyMapper, Function<T, V> valueMapper, Executor executor)`
- `CompletableFuture<Map<K, V>> parallelToMap(Function<T, K> keyMapper, Function<T, V> valueMapper, Executor executor, int parallelism)`
- ...


_parallelToCollection_:

- `CompletableFuture<C> parallelToCollection(Function<T, R> mapper, Supplier<C> collection, Executor executor)`
- `CompletableFuture<C> parallelToCollection(Function<T, R> mapper, Supplier<C> collection, Executor executor, int parallelism)`

_parallelToStream_:

- `CompletableFuture<C> parallelToStream(Function<T, R> mapper, Executor executor)`
- `CompletableFuture<C> parallelToStream(Function<T, R> mapper, Executor executor, int parallelism)`

_parallelToStreamOrdered_:

- `CompletableFuture<C> parallelToStreamOrdered(Function<T, R> mapper, Executor executor)`
- `CompletableFuture<C> parallelToStreamOrdered(Function<T, R> mapper, Executor executor, int parallelism)`

##### Blocking Semantics

If you want to achieve blocking semantics, just add `.join()` straight after collection:

    ...
    .collect(parallelToList(i -> 42, executor))
    .join(); // returns List<Integer>

Above can be used in conjunction with `Stream#collect` as any other `Collector` from `java.util.stream.Collectors`.
 
**By design, it's obligatory to supply a custom `Executor` instance and manage its lifecycle.**

**All those collectors are one-off and should not be reused unless you know what you're doing.**

### Leveraging CompletableFuture

All Parallel Collectors™ expose resulting `Collection` wrapped in `CompletableFuture` instances which provides great flexibility and possibility of working with them in a non-blocking fashion:

    CompletableFuture<List<String>> result = list.stream()
      .collect(parallelToList(i -> foo(i), executor));

This makes it possible to conveniently apply callbacks, and compose with other `CompletableFuture`s:

    list.stream()
      .collect(parallelToList(i -> foo(i), executor))
      .thenAccept(System.out::println)
      .thenRun(() -> System.out.println("Finished!"));
      
Or just `join()` if you just want to block and wait for the result:

    List<String> result = list.stream()
      .collect(parallelToList(i -> foo(i), executor))
      .join();
      
What's more, since JDK9, [you can even provide your own timeout easily](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/CompletableFuture.html#orTimeout(long,java.util.concurrent.TimeUnit)).
      
## In Action

### 1. Parallelize and collect to List

#### with ParallelCollectors™

    Executor executor = ...

    List<String> result = list.stream()
      .collect(parallelToList(i -> foo(i), executor))
      .join(); // on CompletableFuture<Set<String>>

#### with Parallel Streams
    List<String> result = list.parallelStream()
      .map(i -> foo(i)) // runs implicitly on ForkJoinPool.commonPool()
      .collect(Collectors.toList());
      
### 2. Parallelize and collect to List non-blocking

#### with ParallelCollectors™

    Executor executor = ...

    CompletableFuture<List<String>> result = list.stream()
      .collect(parallelToList(i -> foo(i), executor));
    
#### with Parallel Streams
    ¯\_(ツ)_/¯
      
### 3. Parallelize and collect to List on a custom Executor

#### with ParallelCollectors™

    Executor executor = ...

    List<String> result = list.stream()
      .collect(parallelToList(i -> foo(i), executor))
      .join(); // on CompletableFuture<Set<String>>
    
#### with Parallel Streams
    ¯\_(ツ)_/¯

### 4. Parallelize and collect to List and define parallelism

#### with ParallelCollectors™

    Executor executor = ...

    List<String> result = list.stream()
      .collect(parallelToList(i -> foo(i), executor, 42))
      .join(); // on CompletableFuture<Set<String>>
    
#### with Parallel Streams
    System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "42"); 
    
    // global settings ¯\_(ツ)_/¯
    
    List<String> result = list.parallelStream()
      .map(i -> foo(i)) // runs implicitly on ForkJoinPool.commonPool()
      .collect(Collectors.toList());
   
### Maven Dependencies

    <dependency>
        <groupId>com.pivovarit</groupId>
        <artifactId>parallel-collectors</artifactId>
        <version>0.3.0</version>
    </dependency>


##### Gradle

    compile 'com.pivovarit:parallel-collectors:0.3.0'

### Dependencies

None - the library is implemented using core Java libraries.

### Good Practices

- Always provide reasonable timeouts for `CompletableFuture`s [(how-to)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/CompletableFuture.html#orTimeout(long,java.util.concurrent.TimeUnit))
- Name your thread pools - it makes debugging easier [(how-to)](https://stackoverflow.com/a/9748697/2229438)
- Limit the size of a working queue of your thread pool [(source)](https://mechanical-sympathy.blogspot.com/2012/05/apply-back-pressure-when-overloaded.html)
- Always limit the level of parallelism [(source)](https://mechanical-sympathy.blogspot.com/2012/05/apply-back-pressure-when-overloaded.html)
- An unused `ExecutorService` should be shut down to allow reclamation of its resources

### Limitations

- short-circuiting after encountering an exception doesn't actually kill background tasks
- the processing starts after the whole stream is buffered

## Version History

### [0.3.0](https://github.com/pivovarit/parallel-collectors/releases/tag/0.3.0) (05-04-2019)
- Introduced a new API method: `paralleltoStream`
- Introduced a new API method: `paralleltoStreamOrdered`

### [0.2.0](https://github.com/pivovarit/parallel-collectors/releases/tag/0.2.0) (30-03-2019)
- Introduced a new API method `ParallelCollectors.parallelToListOrdered` supporting user-provided `List` implementations
- Introduced a new API method `ParallelCollectors.parallelToMap`

### [0.1.2](https://github.com/pivovarit/parallel-collectors/releases/tag/0.1.2) (26-03-2019)
- Internal exception handling improvements

### [0.1.1](https://github.com/pivovarit/parallel-collectors/releases/tag/0.1.1) (24-03-2019)
- Internal structure improvements
- Thread-safety improvements

### [0.1.0](https://github.com/pivovarit/parallel-collectors/releases/tag/0.1.0) (17-03-2019)
- Introduced a new API method `ParallelCollectors.parallelToListOrdered`

### [0.0.3](https://github.com/pivovarit/parallel-collectors/releases/tag/0.0.3) (21-02-2019)
- Improved performance
- Improved internal implementation
- Relaxed generic type parameters in a backward-compatible manner

### [0.0.2](https://github.com/pivovarit/parallel-collectors/releases/tag/0.0.2) (02-02-2019)
- Fixed the issue with lack of short-circuiting when an exception gets thrown (#140)

### [0.0.1](https://github.com/pivovarit/parallel-collectors/releases/tag/0.0.1) (30-01-2019)
- Changes to the naming convention from `inParallelTo*` to `parallelTo*`
- Improved `UnboundedParallelCollector` implementation
- Improved _JavaDocs_

#### [0.0.1-RC3](https://github.com/pivovarit/parallel-collectors/releases/tag/0.0.1-RC3) (28-01-2019)
* Moved `ThrottlingParallelCollector`'s dispatcher thread to `Collector#finisher`
* `ThrottlingParallelCollector` migrated to use ConcurrentLinkedQueues exclusively
* Added exception-handling-related tests
* Optimized empty `Stream` handling

#### [0.0.1-RC2](https://github.com/pivovarit/parallel-collectors/releases/tag/0.0.1-RC2) (28-01-2019)

* Improved documentation
* Improved internal implementations

#### [0.0.1-RC1](https://github.com/pivovarit/parallel-collectors/releases/tag/parallel-collectors-0.0.1-RC1) (27-01-2019)

* Initial release providing basic functionality

