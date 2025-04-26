package com.pivovarit.collectors;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.invoke.MethodType.methodType;

final class InternalDispatcherResolver {

    private static final MethodHandle NEW_VIRTUAL_THREAD_PER_TASK_EXECUTOR_HANDLE = getNewVirtualThreadPerTaskExecutorHandle();

    private InternalDispatcherResolver() {
    }

    static ExecutorService resolve() {
        if (NEW_VIRTUAL_THREAD_PER_TASK_EXECUTOR_HANDLE != null) {
            try {
                return (ExecutorService) NEW_VIRTUAL_THREAD_PER_TASK_EXECUTOR_HANDLE.invokeExact();
            } catch (Throwable ignore) {
                return Executors.newSingleThreadExecutor();
            }
        } else {
            return Executors.newSingleThreadExecutor();
        }
    }

    private static MethodHandle getNewVirtualThreadPerTaskExecutorHandle() {
        try {
            return MethodHandles.lookup()
              .findStatic(
                Executors.class,
                "newVirtualThreadPerTaskExecutor",
                methodType(ExecutorService.class));
        } catch (Exception ignore) {
        }

        return null;
    }
}
