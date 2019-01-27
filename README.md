# Parallel Collectors for Java 8 Stream API - overcoming limitations of standard Parallel Streams

[![Build Status](https://travis-ci.org/pivovarit/parallel-collectors.svg?branch=master)](https://travis-ci.org/pivovarit/parallel-collectors)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.pivovarit/parallel-collectors/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.pivovarit/parallel-collectors)

## Rationale

Stream API is a great tool for collection processing especially if that involves parallelism of CPU-intensive tasks, for example:

    public static void parallelSetAll(int[] array, IntUnaryOperator generator) {
        Objects.requireNonNull(generator);
        IntStream.range(0, array.length).parallel().forEach(i -> { array[i] = generator.applyAsInt(i); });
    }
    
It's possible because **all tasks managed by parallel Streams are executed on a shared `ForkJoinPool` instance** which was designed for handling this kind of CPU-intensive jobs.
Unfortunately, it's not the best choice for blocking operations - those could easily saturate the common pool:

    List<String> result = list.parallelStream()
      .map(i -> fetchFromDb(i)) // run implicitly on ForkJoinPool.commonPool()
      .collect(Collectors.toList());

A straightforward solution to the problem is to create a separate thread pool for IO-bound tasks and run them there exclusively without impacting the common pool.

**Sadly, Stream API officially only the common `ForkJoinPool` which effectively restricts the applicability of parallelized Streams to CPU-bound jobs.**

There's a trick that allows running parallel Stream in a custom FJP instance but should be avoided:

> Note, however, that this technique of submitting a task to a fork-join pool to run the parallel stream in that pool is an implementation "trick" and is not guaranteed to work. Indeed, the threads or thread pool that is used for execution of parallel streams is unspecified. By default, the common fork-join pool is used, but in different environments, different thread pools might end up being used. 

says [Stuart Marks on StackOverflow](https://stackoverflow.com/questions/28985704/parallel-stream-from-a-hashset-doesnt-run-in-parallel/29272776#29272776)

## Basic API

The library relies on a native `java.util.stream.Collector` mechanism used by Java Stream API which makes it possible to achieve the highest compatibility - **all of them are one-off and should not be reused unless you know what you're doing.**

The only entrypoint is the `com.pivovarit.collectors.ParallelCollectors` class which mimics the semantics of working with `java.util.stream.Collectors` 
and provides static factory methods like:

- `inParallelToList(Executor executor)`
- `inParallelToList(Executor executor, int parallelism)`


- `inParallelToList(Function<T, R> mapper, Executor executor)`
- `inParallelToList(Function<T, R> mapper, Executor executor, int parallelism)`


- `inParallelToSet(Executor executor)`
- `inParallelToSet(Executor executor, int parallelism)`


- `inParallelToSet(Function<T, R> mapper, Executor executor)`
- `inParallelToSet(Function<T, R> mapper, Executor executor, int parallelism)`


- `inParallelToCollection(Supplier<R> collection, Executor executor)`
- `inParallelToCollection(Supplier<R> collection, Executor executor, int parallelism)`


- `inParallelToCollection(Function<T, R> mapper, Supplier<C> collection, Executor executor)`
- `inParallelToCollection(Function<T, R> mapper, Supplier<C> collection, Executor executor, int parallelism)`

Above can be used in conjunction with `Stream#collect` as any other `Collector` from `java.util.stream.Collectors`.
 
**By design, it's obligatory to supply a custom `Executor` instance and manage its lifecycle.**

### Leveraging CompletableFuture

All Parallel Collectors™ don't expose resulting `Collection` directly, instead, they do it with `CompletableFuture` which provides great flexibility and possibility of working with them in a non-blocking fashion:

    CompletableFuture<List<String>> result = list.stream()
      .collect(inParallelToList(i -> fetchFromDb(i), executor))

Which makes it possible to conveniently apply callbacks, and compose with other `CompletableFuture`s:

    list.stream()
      .collect(inParallelToList(i -> fetchFromDb(i), executor))
      .thenAccept(System.out::println)
      .thenRun(() -> System.out.println("Finished!"));
      
## Examples

### 1. Parallelize and collect to List

#### with ParallelCollectors™

    Executor executor = ...

    List<String> result = list.stream()
      .collect(inParallelToList(i -> fetchFromDb(i), executor))
      .join(); // on CompletableFuture<Set<String>>

#### with Parallel Streams
    List<String> result = list.parallelStream()
      .map(i -> fetchFromDb(i)) // runs implicitly on ForkJoinPool.commonPool()
      .collect(Collectors.toList());
      
### 2. Parallelize and collect to List non-blocking

#### with ParallelCollectors™

    Executor executor = ...

    CompletableFuture<List<String>> result = list.stream()
      .collect(inParallelToList(i -> fetchFromDb(i), executor));
    
#### with Parallel Streams
    ¯\_(ツ)_/¯
      
### 3. Parallelize and collect to List on a custom Executor

#### with ParallelCollectors™

    Executor executor = ...

    List<String> result = list.stream()
      .collect(inParallelToList(i -> fetchFromDb(i), executor))
      .join(); // on CompletableFuture<Set<String>>
    
#### with Parallel Streams
    ¯\_(ツ)_/¯

### 4. Parallelize and collect to List and define parallelism

#### with ParallelCollectors™

    Executor executor = ...

    List<String> result = list.stream()
      .collect(inParallelToList(i -> fetchFromDb(i), executor, 42))
      .join(); // on CompletableFuture<Set<String>>
    
#### with Parallel Streams
    System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "42"); 
    
    // global settings ¯\_(ツ)_/¯
    
    List<String> result = list.parallelStream()
      .map(i -> fetchFromDb(i)) // runs implicitly on ForkJoinPool.commonPool()
      .collect(Collectors.toList());
   
### Maven Dependencies

    <dependency>
        <groupId>com.pivovarit</groupId>
        <artifactId>parallel-collectors</artifactId>
        <version>0.0.1-RC1</version>
    </dependency>


##### Gradle

    compile 'com.pivovarit:parallel-collectors:0.0.1-RC1'

### Dependencies

**None - the library is implemented using core Java libraries.**

### Tips

- Name your thread pools
- Limit the size of working queues
- Always limit the parallelism when processing huge streams unless you know what you're doing
- An unused `ExecutorService` should be shut down to allow reclamation of its resources

## Version history

### 0.0.1-RC1 (27-01-2019)

* MVP

