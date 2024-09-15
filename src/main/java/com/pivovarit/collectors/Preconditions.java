package com.pivovarit.collectors;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

final class Preconditions {

    private Preconditions() {
    }

    static void requireValidExecutor(Executor executor) {
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
