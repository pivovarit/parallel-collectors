package com.pivovarit.collectors;

import java.util.concurrent.Executor;

final class Options {

  private Options() {}

  sealed interface CollectingOption {}

  sealed interface StreamingOption extends CollectingOption {}

  static ThreadPool executor(Executor executor) {
    return new ThreadPool(executor);
  }

  static Batched batched() {
    return new Batched();
  }

  static Parallelism parallelism(int parallelism) {
    return new Parallelism(parallelism);
  }

  static Ordered ordered() {
    return new Ordered();
  }

  record Ordered() implements StreamingOption {}

  record Batched() implements StreamingOption, CollectingOption {}

  record ThreadPool(Executor executor) implements StreamingOption, CollectingOption {
    public ThreadPool {
      Preconditions.requireValidExecutor(executor);
    }
  }

  record Parallelism(int parallelism) implements StreamingOption, CollectingOption {
    public Parallelism {
      Preconditions.requireValidParallelism(parallelism);
    }
  }
}
