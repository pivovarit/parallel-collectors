package com.pivovarit.collectors;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

final class Preconditions {

    private Preconditions() {
    }

    static void requireValidParallelism(int parallelism) {
        if (parallelism < 1) {
            throw new IllegalArgumentException("Parallelism can't be lower than 1");
        }
    }

    static void requireValidExecutor(Executor executor) {
        Objects.requireNonNull(executor, "Executor can't be null");
        if (executor instanceof ThreadPoolExecutor tpe) {
            switch (tpe.getRejectedExecutionHandler()) {
                case ThreadPoolExecutor.DiscardPolicy __ ->
                  throw new IllegalArgumentException("Executor's RejectedExecutionHandler can't discard tasks");
                case ThreadPoolExecutor.DiscardOldestPolicy __ ->
                  throw new IllegalArgumentException("Executor's RejectedExecutionHandler can't discard tasks");
                default -> {
                    // no-op
                }
            }
        }
    }
}
