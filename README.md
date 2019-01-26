# parallel-collectors

[![Build Status](https://travis-ci.org/pivovarit/parallel-collectors.svg?branch=master)](https://travis-ci.org/pivovarit/parallel-collectors)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.pivovarit/parallel-collectors/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.pivovarit/parallel-collectors)

# Parallel Collection Processing

## Rationale

Stream API is a great tool for collection processing especially if that involves parallelizing CPU-intensive tasks, for example:

    public static void parallelSetAll(int[] array, IntUnaryOperator generator) {
        Objects.requireNonNull(generator);
        IntStream.range(0, array.length).parallel().forEach(i -> { array[i] = generator.applyAsInt(i); });
    }
    
It's possible because all tasks managed by parallel Streams are executed on a shared `ForkJoinPool` instance which was designed for handling this kind of CPU-intensive jobs.
Unfortunately, it's not the best choice for blocking operations - those could easily saturate the common pool.

The standard way of dealing with the problem is to create a separate thread pool for IO-bound tasks and run them there exclusively.
As a matter of fact, Stream API supports only the common `ForkJoinPool` which restricts effectively the applicability of parallelized Streams to CPU-bound jobs.

## Basic API

The main (and the only) entrypoint to the library is the `com.pivovarit.collectors.ParallelCollectors` class which mimics the semantics of `java.util.stream.Collectors` 
and provides collectors:

- `inParallelToList(Executor executor)`
- `inParallelToList(Executor executor, int parallelism)`
- `inParallelToList(Function<T, R> operation, Executor executor)`
- `inParallelToList(Function<T, R> operation, Executor executor, int parallelism)`

- `inParallelToSet(Executor executor)`
- `inParallelToSet(Executor executor, int parallelism)`
- `inParallelToSet(Function<T, R> operation, Executor executor)`
- `inParallelToSet(Function<T, R> operation, Executor executor, int parallelism)`

- `inParallelToCollection(Supplier<R> collection, Executor executor)`
- `inParallelToCollection(Supplier<R> collection, Executor executor, int parallelism)`
- `inParallelToCollection(Function<T, R> operation, Supplier<C> collection, Executor executor)`
- `inParallelToCollection(Function<T, R> operation, Supplier<C> collection, Executor executor, int parallelism)`

Above can be used in conjunction with `Stream#collect` as any other `Collector` from `java.util.stream.Collectors`. 
It's obligatory to supply custom `Executor` instance and manage its own lifecycle.

### Examples

```
List<String> result = list.stream()
  .collect(inParallelToList(i -> fetchFromDb(i), executor))
  .join();
```
```
CompletableFuture<List<String>> futureResult = list.stream()
  .collect(inParallelToList(i -> fetchFromDb(i), executor));
```

## Implementation details

In order to ensure the highest compatibility, the library relies on a native `Collector` mechanism used by Java Stream API.

## Dependencies

None - the library is implemented using core Java libraries.

### Maven
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
## Version history

### TODO 0.0.1-SNAPSHOT (23-01-2010)

* MVP

