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

## Basic API

The library relies on a native `java.util.stream.Collector` mechanism used by Java Stream API which makes it possible to achieve the highest compatibility.

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

#### Leveraging CompletableFuture

All Parallel Collectors™ don't expose resulting `Collection` directly, instead, they do it with `CompletableFuture` instead:

    CompletableFuture<List<String>> result = list.stream()
      .collect(inParallelToList(i -> fetchFromDb(i), executor))

Which makes it possible to conveniently apply callbacks, and compose with other `CompletableFuture`s:

    list.stream()
      .collect(inParallelToList(i -> fetchFromDb(i), executor))
      .thenAccept(System.out::println)
      .thenRun(() -> System.out.println("Finished!"));
      
## Examples

### 1. Fetch in parallel and collect to List

#### with Parallel Streams
    List<String> result = list.parallelStream()
      .map(i -> fetchFromDb(i)) // run implicitly on ForkJoinPool.commonPool()
      .collect(Collectors.toList());


#### with ParallelCollectors™

    Executor executor = ...

    List<String> result = list.stream()
      .collect(inParallelToList(i -> fetchFromDb(i), executor))
      .join(); // on CompletableFuture<Set<String>>
      
    executor.shutdown(); // if not needed
    
### 2. Fetch in parallel and collect to Set

#### with Parallel Streams
    Set<String> result = list.parallelStream()
      .map(i -> fetchFromDb(i)) // run implicitly on ForkJoinPool.commonPool()
      .collect(toSet());


#### with ParallelCollectors™

    Executor executor = ...

    Set<String> result = list.stream()
      .collect(inParallelToSet(i -> fetchFromDb(i), executor))
      .join(); // on CompletableFuture<List<String>>
      
    executor.shutdown(); // if not needed
    
### 3. Fetch in parallel and collect to a custom Collection

#### with Parallel Streams
    List<String> result = list.parallelStream()
      .map(i -> fetchFromDb(i)) // run implicitly on ForkJoinPool.commonPool()
      .collect(toCollection(LinkedList::new));


#### with ParallelCollectors™

    Executor executor = ...

    List<String> result = list.stream()
      .collect(inParallelToCollection(i -> fetchFromDb(i), LinkedList::new, executor))
      .join(); // on CompletableFuture<LinkedList<String>>
      
    executor.shutdown(); // if not needed
    
### 4. Fetch in parallel and limit parallelism

#### with Parallel Streams
    ¯\_(ツ)_/¯

#### with ParallelCollectors™

    Executor executor = ...

    Set<String> result = list.stream()
      .collect(inParallelToList(i -> fetchFromDb(i), executor, 2)) // max 2 items processed at once
      .join(); // on CompletableFuture<Set<String>>
      
    executor.shutdown(); // if not needed

### Maven Dependencies
```
<repositories>
    <repository>
        <id>snapshots-repo</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        <releases><enabled>false</enabled></releases>
        <snapshots><enabled>true</enabled></snapshots>
    </repository>
</repositories>
```
```
<dependency>
    <groupId>com.pivovarit</groupId>
    <artifactId>parallel-collectors</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Dependencies

**None - the library is implemented using core Java libraries.**

### Tips

- Name your thread pools
- Limit the size of the working queue
- Always Limit the parallelism when processing huge streams unless you know what you're doing
- Release resources after usage if a submitted pool is no longer in use

## Version history

### TODO 0.0.1-SNAPSHOT (23-01-2010)

* MVP

