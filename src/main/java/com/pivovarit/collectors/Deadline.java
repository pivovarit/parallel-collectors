/*
 * Copyright 2014-2026 Grzegorz Piwowarek, https://4comprehension.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pivovarit.collectors;

import java.util.concurrent.atomic.AtomicLong;

final class Deadline {

    private static final long UNSET = Long.MIN_VALUE;

    private final long timeoutNanos;
    private final AtomicLong deadlineNanos = new AtomicLong(UNSET);

    Deadline(long timeoutNanos) {
        this.timeoutNanos = timeoutNanos;
    }

    long remainingNanos() {
        long deadline = deadlineNanos.get();
        if (deadline == UNSET) {
            long candidate = System.nanoTime() + timeoutNanos;
            deadline = deadlineNanos.compareAndSet(UNSET, candidate) ? candidate : deadlineNanos.get();
        }
        return Math.max(0, deadline - System.nanoTime());
    }
}
