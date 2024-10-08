# Version History

### [3.2.0](https://github.com/pivovarit/parallel-collectors/releases/tag/3.2.0) (17-08-2024)
- add new API methods defaulting to Virtual Threads but allowing setting max parallelism

### [3.1.0](https://github.com/pivovarit/parallel-collectors/releases/tag/3.1.0) (01-05-2024)
- use a dedicated dispatcher Virtual Thread to control parallelism
- reject illegal `RejectedExecutionHandlers` at best-effort basis

### [3.0.0](https://github.com/pivovarit/parallel-collectors/releases/tag/3.0.0) (30-01-2024)
- full Virtual Thread support with new API methods
- deprecated methods removed
- `module-info`

### [2.6.1](https://github.com/pivovarit/parallel-collectors/releases/tag/2.6.1) (01-05-2024)
- Fixes a regression which caused undesired pollution of the provided executor

### [2.6.0](https://github.com/pivovarit/parallel-collectors/releases/tag/2.6.0) (27-01-2023)
- `Dispatcher` no longer relies on its own thread to dispatch tasks, but uses caller thread
- default parallelism can't be smaller than 4

### [2.5.0](https://github.com/pivovarit/parallel-collectors/releases/tag/2.5.0) (11-02-2021)
- fixed a bug causing lack of eager consumption of items when processed using streaming collectors
- optimized batching logic when collection size is equal to parallelism

### [2.4.1](https://github.com/pivovarit/parallel-collectors/releases/tag/2.4.1) (22-11-2020)
- removed redundant JMH artifacts from the jar
- add missing project metadata to the _MANIFEST.MF_ file

### [2.4.0](https://github.com/pivovarit/parallel-collectors/releases/tag/2.4.0) (26-09-2020)
- removed a redundant intermediate `CompletableFuture` instance creation
- streaming collectors offload the work to the calling thread instead of creating a new thread

### [2.3.3](https://github.com/pivovarit/parallel-collectors/releases/tag/2.3.3) (25-04-2020)
- fixed potential concurrency issue when dispatcher thread gets interrupted before it starts waiting on `Semaphore#acquire`
- ensured progress when poison pill can't be added to the working queue

### [2.3.2](https://github.com/pivovarit/parallel-collectors/releases/tag/2.3.2) (27-02-2020)
- fixed unwanted lazy semantics when parallelism == 1

### [2.3.1](https://github.com/pivovarit/parallel-collectors/releases/tag/2.3.1) (22-02-2020)
- fixed ParallelCollectors.Batching visibility issues 

### [2.3.0](https://github.com/pivovarit/parallel-collectors/releases/tag/2.3.0) (09-02-2020)
- introduced ParallelCollectors#toFuture
- fixed the issue with improper ordering of batched streams
- introduced multiple performance improvements

### [2.2.0](https://github.com/pivovarit/parallel-collectors/releases/tag/2.2.0) (19-01-2020)
Introduced multiple performance improvements:
- avoiding excessive allocations
- optimizing edge cases where `parallelism == 1`

### [2.1.0](https://github.com/pivovarit/parallel-collectors/releases/tag/2.1.0) (11-12-2019)
Introduced a new subset of drop-in replacement collectors `ParallelCollectors.Batching`. 

These implement a different task-splitting strategy where operations are performed in batches and not separately (one object in a thread pool's worker queue represents a batch of operations to be performed by a single thread):

`ParallelCollectors.Batching`:
- `parallel(...)`
- `parallelToStream(...)`
- `parallelToOrderedStream(...)`

### [2.0.0](https://github.com/pivovarit/parallel-collectors/releases/tag/2.0.0) (20-11-2019)
**Redesigned and simplified the public-facing API** to enable reuse of `java.util.stream.Collector` instances for providing custom reduction strategies which enabled reduction of the public API from 13 methods to just 4:

-  `CompletableFuture<List<T>> parallel(Function, Collector, Executor, parallelism)`
-  `CompletableFuture<Stream<T>> parallel(Function, Executor, parallelism)`

-  `Stream<T> parallelToStream(Function, Executor, parallelism)`
-  `Stream<T> parallelToOrderedStream(Function, Executor, parallelism)`
----

### [1.2.1](https://github.com/pivovarit/parallel-collectors/releases/tag/1.2.1) (17-11-2019)
- Adjusted visibility modifiers of some API methods

### [1.2.0](https://github.com/pivovarit/parallel-collectors/releases/tag/1.2.0) (16-11-2019)
- Added new `parallel` methods that are interoperable with `java.util.stream.Collector`s
- Simplified internal implementations
- Deprecated `parallelTo(List/Set/Map/Collection)` methods in favor of the above one
----

### [1.1.0](https://github.com/pivovarit/parallel-collectors/releases/tag/1.1.0) (24-06-2019)
- Introduced interruptions/short-circuiting of tasks when at least one exceptional completion happens
- Deprecated `parallelMap` and `parallelMapOrdered` and replaced them with `parallel` and `parallelOrdered`
----

### [1.0.3](https://github.com/pivovarit/parallel-collectors/releases/tag/1.0.3) (09-06-2019)
- Reimplemented `CompletionOrderSpliterator` with throughput in mind

### [1.0.2](https://github.com/pivovarit/parallel-collectors/releases/tag/1.0.2) (03-06-2019)
- Fixed improperly configured `CompletionOrderSpliterator`

### [1.0.1](https://github.com/pivovarit/parallel-collectors/releases/tag/1.0.1) (02-05-2019)
- Removal of redundant/unused code

### [1.0.0](https://github.com/pivovarit/parallel-collectors/releases/tag/1.0.0) (30-04-2019)
- Removed deprecated methods
- Introduced limited default parallelism (used to be unlimited)
- Introduced new API methods: `parallelMap`/`parallelMapOrdered`
----

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
