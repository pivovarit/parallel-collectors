# Potential Bugs in parallel-collectors

This document identifies potential bugs, issues, and areas of concern in the parallel-collectors codebase.

## Summary

After analyzing the codebase, I've identified **10 potential bugs** categorized by severity:
- **Critical**: 2 bugs
- **High**: 3 bugs  
- **Medium**: 3 bugs
- **Low**: 2 bugs

---

## Critical Issues

### 1. **Race Condition in Dispatcher.start() - Potential Double Start**
**Location**: `Dispatcher.java:47-81`

**Issue**: While `started.getAndSet(true)` prevents multiple threads from creating multiple dispatcher threads, there's a potential race condition where tasks can be enqueued before the dispatcher thread fully starts processing the queue.

**Code**:
```java
void start() {
    if (!started.getAndSet(true)) {
        dispatcherThreadFactory.newThread(() -> {
            try {
                while (true) {
                    switch (workingQueue.take()) {
                        // processing
                    }
                }
```

**Problem**: 
- Thread A calls `start()` and sets `started = true`
- Thread B calls `enqueue()` and adds items to the queue
- Thread A's dispatcher thread may not have started the `while(true)` loop yet
- Although this is unlikely to cause data loss (LinkedBlockingQueue is thread-safe), it could cause timing issues

**Severity**: Critical (concurrency issue)

**Recommendation**: Add a CountDownLatch or similar synchronization to ensure the dispatcher thread is fully running before returning from `start()`.

---

### 2. **Semaphore Leak on Rejected Execution in Retry Logic**
**Location**: `Dispatcher.java:153-160`

**Issue**: If both retry attempts fail with `RejectedExecutionException`, the semaphore permit acquired in line 56 is never released, leading to a semaphore leak.

**Code**:
```java
private static void retry(Runnable runnable) {
    try {
        runnable.run();
    } catch (RejectedExecutionException e) {
        Thread.onSpinWait();
        runnable.run();  // If this fails again, semaphore is never released
    }
}
```

**Problem**:
- Semaphore is acquired at line 56: `limiter.acquire()`
- If the executor rejects the task on first attempt, we retry once
- If the second attempt also fails, the exception propagates without releasing the semaphore
- This leads to gradual exhaustion of available permits

**Severity**: Critical (resource leak)

**Recommendation**: Wrap the retry logic in a try-finally block to ensure semaphore release on failure, or handle the second RejectedExecutionException explicitly.

---

## High Priority Issues

### 3. **Missing Null Check in ParallelCollectors.parallelBy() Methods**
**Location**: `ParallelCollectors.java:172-173, 255`

**Issue**: Some `parallelBy()` overloads check for null classifier, but line 172 doesn't check before using it.

**Code**:
```java
// Line 165-174: Missing classifier null check
public static <T, K, R, RR> Collector<T, ?, CompletableFuture<RR>> parallelBy(
  Function<? super T, ? extends K> classifier,
  Function<? super T, ? extends R> mapper,
  Collector<Grouped<K, R>, ?, RR> collector,
  int parallelism) {

    Objects.requireNonNull(collector, "collector cannot be null");
    // Missing: Objects.requireNonNull(classifier, "classifier cannot be null");

    return Factory.collectingBy(classifier, ...);
}
```

**Problem**: Inconsistent null checking across overloaded methods. Some methods check classifier for null (lines 92, 255, 334) while others don't (line 172).

**Severity**: High (null pointer exception risk)

**Recommendation**: Add `Objects.requireNonNull(classifier, "classifier cannot be null");` to line 172.

---

### 4. **Unchecked Cast Potential ClassCastException in Factory.collectingBy()**
**Location**: `Factory.java:41`

**Issue**: Unsafe cast without verification that could lead to ClassCastException at runtime.

**Code**:
```java
return Factory.collectingBy(classifier, (Function<Stream<Grouped<K, R>>, RR>) s -> s.collect(collector), mapper, options);
```

And at line 41:
```java
e -> new Grouped<>(e.getKey(), e.getValue().stream()
    .map(mapper.andThen(a -> (R) a))  // Unchecked cast
    .toList()), options))
```

**Problem**: 
- The cast `(R) a` at line 41 assumes the mapper returns type R
- While this should be guaranteed by the type system, the explicit cast could hide issues
- If mapper throws or returns null where not expected, this could cause issues

**Severity**: High (potential ClassCastException)

**Recommendation**: Review type safety guarantees and add defensive checks if needed.

---

### 5. **BatchingCollector Logic Bug - Incorrect Size Comparison**
**Location**: `AsyncParallelCollector.java:129-131`

**Issue**: The condition `items.size() == parallelism` seems incorrect for determining whether batching is needed.

**Code**:
```java
@Override
public Function<ArrayList<T>, CompletableFuture<C>> finisher() {
    return items -> {
        if (items.size() == parallelism) {
            return items.stream()
              .collect(AsyncParallelCollector.from(task, finalizer, executor, parallelism));
        } else {
            return partitioned(items, parallelism)
              .collect(AsyncParallelCollector.from(batch -> {
                  // batched processing
              }));
        }
    };
}
```

**Problem**:
- If `items.size()` equals `parallelism`, the code processes each item individually
- If `items.size()` does NOT equal `parallelism`, it batches the items
- This seems backwards - batching should happen when there are MORE items than parallelism, not fewer
- Example: With parallelism=4 and 100 items, this will batch. But with exactly 4 items, it won't batch - which is odd for a "BatchingCollector"

**Severity**: High (logic error)

**Recommendation**: Review the batching logic. The condition should likely be `items.size() <= parallelism` or the branches should be swapped.

---

## Medium Priority Issues

### 6. **Potential Memory Leak - DEFAULT_EXECUTOR Never Shutdown**
**Location**: `ConfigProcessor.java:14-16`

**Issue**: The static `DEFAULT_EXECUTOR` is never shut down, which can prevent JVM shutdown.

**Code**:
```java
private static final ExecutorService DEFAULT_EXECUTOR = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
  .name("parallel-collectors-", 0)
  .factory());
```

**Problem**:
- Virtual thread executors should be closed when no longer needed
- As a static field, this executor persists for the life of the JVM
- While virtual threads are lightweight, keeping an executor open indefinitely could cause issues

**Severity**: Medium (resource management)

**Recommendation**: 
- Document that users should use their own executor for production use
- Consider using a WeakReference or providing a shutdown hook
- Or document this as intentional behavior for the default case

---

### 7. **CompletionOrderSpliterator Doesn't Handle Cancelled Futures**
**Location**: `CompletionOrderSpliterator.java:26-42`

**Issue**: If a CompletableFuture is cancelled, the `join()` call will throw CancellationException, but there's no handling for this case.

**Code**:
```java
@Override
public boolean tryAdvance(Consumer<? super T> action) {
    return remaining > 0
      ? nextCompleted().thenAccept(action).thenApply(__ -> true).join()
      : false;
}

private CompletableFuture<T> nextCompleted() {
    try {
        var next = completed.take();
        remaining--;
        return next;  // If this future was cancelled, join() will throw
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
    }
}
```

**Problem**: 
- If a future completes exceptionally or is cancelled, `join()` will throw
- This will propagate up and potentially break the stream
- While this might be intended behavior, it's not documented

**Severity**: Medium (error handling)

**Recommendation**: Document the exception behavior or add explicit handling for cancellation.

---

### 8. **Dispatcher Interrupt Clears Working Queue Without Canceling All Tasks**
**Location**: `Dispatcher.java:115-126`

**Issue**: When an exception occurs, the `interrupt()` method attempts to cancel tasks in the queue, but tasks already submitted to the executor are not cancelled.

**Code**:
```java
private void interrupt(Throwable e) {
    completionSignaller.completeExceptionally(e);

    for (var item : workingQueue) {
        switch (item) {
            case DispatchItem.Task task -> task.cancel();
            case DispatchItem.Stop ignored -> {
                // nothing to cancel
            }
        }
    }
}
```

**Problem**:
- Only cancels tasks still in the `workingQueue`
- Tasks already submitted to `executor` (line 62-70) are not tracked and cannot be cancelled
- This means work continues even after an error is detected
- The documentation claims "short-circuiting" but this implementation is incomplete

**Severity**: Medium (incomplete short-circuiting)

**Recommendation**: Track submitted tasks and cancel them on interrupt, or document the limitation.

---

## Low Priority Issues

### 9. **BatchingSpliterator Division By Zero Risk**
**Location**: `BatchingSpliterator.java:74`

**Issue**: If `chunks` reaches 0, the calculation will divide by zero.

**Code**:
```java
chunkSize = (int) Math.ceil(((double) (source.size() - consumed)) / --chunks);
```

**Problem**:
- Line 74 pre-decrements `chunks` (--chunks)
- If `chunks` was 1, it becomes 0, causing division by zero
- The guard `chunks != 0` on line 70 checks before decrement, so there's a race

**Severity**: Low (edge case, protected by guard condition)

**Recommendation**: Review the logic flow to ensure chunks can never be 0 when this line executes. Add an assertion if confident.

---

### 10. **AsyncCollector Catches Generic Exception**
**Location**: `AsyncCollector.java:37-41`

**Issue**: Catching generic `Exception` is overly broad and could hide unexpected errors.

**Code**:
```java
@Override
public Function<Stream.Builder<T>, CompletableFuture<RR>> finisher() {
    return acc -> {
        try {
            return CompletableFuture.supplyAsync(() -> processor.apply(acc.build().map(mapper)), executor);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    };
}
```

**Problem**:
- Catches all Exceptions, including RuntimeExceptions that might indicate programming errors
- Could mask bugs like NullPointerException, IllegalStateException, etc.
- The intent seems to be to wrap checked exceptions, but `supplyAsync` doesn't throw checked exceptions

**Severity**: Low (code quality)

**Recommendation**: 
- Remove the try-catch since `supplyAsync` handles exceptions internally
- Or be more specific about what exceptions to catch

---

## Additional Observations

### Thread Safety Concerns
1. The `Dispatcher` class uses multiple synchronization mechanisms (AtomicBoolean, Semaphore, BlockingQueue) - ensure they work together correctly under all race conditions.

### Documentation Gaps
1. The "short-circuiting" behavior isn't fully implemented as documented
2. Exception handling behavior could be better documented
3. Thread safety guarantees should be explicitly stated

### Testing Recommendations
1. Add tests for `RejectedExecutionException` with semaphore checking
2. Add tests for concurrent `start()` calls
3. Add tests for cancelled CompletableFutures
4. Add edge case tests for BatchingSpliterator with chunks=1
5. Add tests for null classifiers in all parallelBy() variants

---

## Conclusion

The codebase is generally well-structured, but has several potential issues related to:
- **Concurrency**: Race conditions, semaphore leaks, incomplete cancellation
- **Error Handling**: Missing null checks, overly broad exception catching
- **Logic Errors**: Questionable batching conditions
- **Resource Management**: Static executor not shutdown

Most critical issues are around the `Dispatcher` class, which handles the core concurrency concerns. I recommend prioritizing fixes for the semaphore leak (#2) and race condition (#1) issues.
