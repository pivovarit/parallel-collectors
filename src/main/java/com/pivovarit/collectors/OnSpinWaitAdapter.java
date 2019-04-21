package com.pivovarit.collectors;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodType.methodType;

/**
 * @author Grzegorz Piwowarek
 */
final class OnSpinWaitAdapter {
    private static final MethodHandle ON_SPIN_WAIT_HANDLE = resolveOnSpinWait();

    private OnSpinWaitAdapter() {
    }

    private static MethodHandle resolveOnSpinWait() {
        try {
            return MethodHandles.lookup().findStatic(Thread.class, "onSpinWait", methodType(void.class));
        } catch (Throwable ignore) {
        }

        return null;
    }

    static void onSpinWait() {
        if (ON_SPIN_WAIT_HANDLE != null) {
            try {
                ON_SPIN_WAIT_HANDLE.invokeExact();
            } catch (Throwable ignore) {
            }
        }
    }
}
