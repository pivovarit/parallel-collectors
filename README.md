# parallel-collectors

[![Build Status](https://travis-ci.org/pivovarit/parallel-collectors.svg?branch=master)](https://travis-ci.org/pivovarit/parallel-collectors)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

# Parallel Collection Processing

## Rationale

## Basic API

`com.pivovarit.ParallelCollectors` class serves as the main entrypoint to the library (in a similar manner that `java.util.stream.Collectors` do.

It features static factory methods like:
- `inParallelToList()`
- `inParallelToSet()`
- `inParallelToCollection()`

Above (along with customizable overloads) can be used in conjunction with `Stream#collect` as any other `Collector` from `java.util.stream.Collectors`. It's obligatory to supply custom `Executor` instance and manage its own lifecycle.

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

## Version history

### TODO 0.0.1-SNAPSHOT (23-01-2010)

* MVP

