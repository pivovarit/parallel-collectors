# Java Stream API Parallel Collectors - overcoming limitations of standard Parallel Streams

 <a href="https://github.com/actions/toolkit"><img alt="GitHub Actions status" src="https://github.com/pivovarit/parallel-collectors/workflows/build/badge.svg"></a>
  <a href="https://pivovarit.github.io/parallel-collectors/"><img alt="GitHub Actions status" src="https://github.com/pivovarit/parallel-collectors/workflows/pitest/badge.svg"></a>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.pivovarit/parallel-collectors/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.pivovarit/parallel-collectors)

<a href="https://twitter.com/intent/follow?screen_name=pivovarit">
        <img src="https://img.shields.io/twitter/follow/pivovarit.svg?style=social&logo=twitter"
            alt="follow on Twitter"></a>
            
[![Stargazers over time](https://starchart.cc/pivovarit/parallel-collectors.svg)](https://starchart.cc/pivovarit/parallel-collectors)

Parallel Collectors is a toolkit easing parallel collection processing in Java using Stream API... but without limitations imposed by standard Parallel Streams.

    list.stream()
      .collect(parallel(i -> foo(i), toList(), executor, parallelism))
        .orTimeout(1000, MILLISECONDS)
        .thenAcceptAsync(System.out::println, otherExecutor)
        .thenRun(() -> System.out.println("Finished!"));
      
They are:
- lightweight (yes, you could achieve the same with Project Reactor, but that's often a hammer way too big for the job)
- powerful (combined power of `Stream` API and `CompletableFuture`s allows to specify timeouts, compose with other `CompletableFuture`s, or just perform the whole processing asynchronously)
- configurable (it's possible to provide your own `Executor`, `parallelism`)
- non-blocking (no need to block the calling thread while waiting for the result to arrive)
- short-circuiting (if one of the operations raises an exception, remaining tasks will get interrupted)  
- non-invasive (they are just custom implementations of `Collector` interface, no magic inside, zero-dependencies)
- versatile (missing an API for your use case? process the resulting Stream with the whole generosity of Stream API by reusing already available `Collectors`)

### Maven Dependencies

    <dependency>
        <groupId>com.pivovarit</groupId>
        <artifactId>parallel-collectors</artifactId>
        <version>2.5.0</version>
    </dependency>

##### Gradle

    compile 'com.pivovarit:parallel-collectors:2.5.0'

## Philosophy

Parallel Collectors are unopinionated by design, so it's up to their users to use them responsibly, which involves things like:
- proper configuration of a provided `Executor` and its lifecycle management
- choosing the appropriate parallelism level
- making sure that the tool is applied in the right context

Make sure to read API documentation before using these in production.

## Basic API

The main entrypoint is the `com.pivovarit.collectors.ParallelCollectors` class - which follows the convention established by `java.util.stream.Collectors` and features static factory methods returning custom `java.util.stream.Collector` implementations spiced up with parallel processing capabilities.

By design, it's obligatory to supply a custom `Executor` instance and manage its lifecycle.

All parallel collectors are one-off and must not be reused.

### Available Collectors:

-  `CompletableFuture<Collection<T>> parallel(Function, Collector, Executor, parallelism)`
-  `CompletableFuture<Stream<T>> parallel(Function, Executor, parallelism)`

-  `Stream<T> parallelToStream(Function, Executor, parallelism)`
-  `Stream<T> parallelToOrderedStream(Function, Executor, parallelism)`

#### Batching Collectors
By default, all `ExecutorService` threads _compete_ for each task separately - which results in a basic form of _work-stealing_, which, unfortunately, is not free, but can decrease processing time for subtasks with varying processing time.

However, if the processing time for all subtasks is similar, it might be better to distribute tasks in batches to avoid excessive contention:

![](docs/batching.png)

Batching alternatives are available under the `ParallelCollectors.Batching` namespace.


### Leveraging CompletableFuture

Parallel Collectors™ expose results wrapped in `CompletableFuture` instances which provides great flexibility and possibility of working with them in a non-blocking fashion:

    CompletableFuture<List<String>> result = list.stream()
      .collect(parallel(i -> foo(i), toList(), executor));

This makes it possible to conveniently apply callbacks, and compose with other `CompletableFuture`s:

    list.stream()
      .collect(parallel(i -> foo(i), toSet(), executor))
      .thenAcceptAsync(System.out::println, otherExecutor)
      .thenRun(() -> System.out.println("Finished!"));
      
Or just `join()` if you just want to block the calling thread and wait for the result:

    List<String> result = list.stream()
      .collect(parallel(i -> foo(i), toList(), executor))
      .join();
      
What's more, since JDK9, [you can even provide your own timeout easily](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/CompletableFuture.html#orTimeout(long,java.util.concurrent.TimeUnit)).
      
## Examples

##### 1. Apply `i -> foo(i)` in parallel on a custom `Executor` and collect to `List`

    Executor executor = ...

    CompletableFuture<List<String>> result = list.stream()
      .collect(parallel(i -> foo(i), toList(), executor));
      
##### 2. Apply `i -> foo(i)` in parallel on a custom `Executor` with max parallelism of 4 and collect to `Set`

    Executor executor = ...

    CompletableFuture<Set<String>> result = list.stream()
      .collect(parallel(i -> foo(i), toSet(), executor, 4));
      
##### 3. Apply `i -> foo(i)` in parallel on a custom `Executor` and collect to `LinkedList`

    Executor executor = ...

    CompletableFuture<List<String>> result = list.stream()
      .collect(parallel(i -> foo(i), toCollection(LinkedList::new), executor));
      
##### 4. Apply `i -> foo(i)` in parallel on a custom `Executor` and stream results in completion order

    Executor executor = ...

    list.stream()
      .collect(parallelToStream(i -> foo(i), executor))
      .forEach(i -> ...);
      
##### 5. Apply `i -> foo(i)` in parallel on a custom `Executor` and stream results in original order

    Executor executor = ...

    list.stream()
      .collect(parallelToOrderedStream(i -> foo(i), executor))
      .forEach(i -> ...);

## Rationale

Stream API is a great tool for collection processing, especially if you need to parallelize execution of CPU-intensive tasks, for example:

    public static void parallelSetAll(int[] array, IntUnaryOperator generator) {
        Objects.requireNonNull(generator);
        IntStream.range(0, array.length).parallel().forEach(i -> { array[i] = generator.applyAsInt(i); });
    }
    
**However, Parallel Streams execute tasks on a shared `ForkJoinPool` instance**.
 
Unfortunately, it's not the best choice for running blocking operations even when using `ManagedBlocker` - [as explained here by Tagir Valeev](https://stackoverflow.com/a/37518272/2229438)) - this could easily lead to the saturation of the common pool, and to a performance degradation of everything that uses it.

For example:

    List<String> result = list.parallelStream()
      .map(i -> foo(i)) // runs implicitly on ForkJoinPool.commonPool()
      .collect(Collectors.toList());

In order to avoid such problems, **the solution is to isolate blocking tasks** and run them on a separate thread pool... but there's a catch.

**Sadly, Streams can only run parallel computations on the common `ForkJoinPool`** which effectively restricts the applicability of them to CPU-bound jobs.

However, there's a trick that allows running parallel Streams in a custom FJP instance... but it's not considered reliable:

> Note, however, that this technique of submitting a task to a fork-join pool to run the parallel stream in that pool is an implementation "trick" and is not guaranteed to work. Indeed, the threads or thread pool that is used for execution of parallel streams is unspecified. By default, the common fork-join pool is used, but in different environments, different thread pools might end up being used. 

Says [Stuart Marks on StackOverflow](https://stackoverflow.com/questions/28985704/parallel-stream-from-a-hashset-doesnt-run-in-parallel/29272776#29272776). 

Not even mentioning that this approach was seriously flawed before JDK-10 - if a `Stream` was targeted towards another pool, splitting would still need to adhere to the parallelism of the common pool, and not the one of the targeted pool [[JDK8190974]](https://bugs.openjdk.java.net/browse/JDK-8190974).
   
### Dependencies

None - the library is implemented using core Java libraries.

### Limitations

Upstream `Stream` is always evaluated as a whole, even if the following operation is short-circuiting.
This means that none of these should be used for working with infinite streams.

This limitation is imposed by the design of the `Collector` API.

### Good Practices

- Consider providing reasonable timeouts for `CompletableFuture`s in order to not block for unreasonably long in case when something bad happens [(how-to)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/CompletableFuture.html#orTimeout(long,java.util.concurrent.TimeUnit))
- Name your thread pools - it makes debugging easier [(how-to)](https://stackoverflow.com/a/9748697/2229438)
- Limit the size of a working queue of your thread pool [(source)](https://mechanical-sympathy.blogspot.com/2012/05/apply-back-pressure-when-overloaded.html)
- Limit the level of parallelism [(source)](https://mechanical-sympathy.blogspot.com/2012/05/apply-back-pressure-when-overloaded.html)
- A no-longer-used `ExecutorService` should be shut down to allow reclamation of its resources
- Keep in mind that `CompletableFuture#then(Apply|Combine|Consume|Run|Accept)` might be executed by the calling thread. If this is not suitable, use `CompletableFuture#then(Apply|Combine|Consume|Run|Accept)Async` instead, and provide a custom executor instance.

## Words of Caution

Even if this tool makes it easy to parallelize things, it doesn't always mean that you should. **Parallelism comes with a price that can be often higher than not using it at all.** Threads are expensive to create, maintain and switch between, and you can only create a limited number of them.

It's essential to follow up on the root cause and double-check if parallelism is the way to go.

**It often turns out that the root cause can be addressed by using a simple JOIN statement, batching, reorganizing your data... or even just by choosing a different API method.**

----
See [CHANGELOG.MD](https://github.com/pivovarit/parallel-collectors/blob/master/CHANGELOG.MD) for a complete version history.
      

