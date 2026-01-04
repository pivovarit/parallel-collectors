package com.pivovarit.collectors;

import java.lang.reflect.Field;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DispatcherSemaphoreLeakTest {

    @Test
    void shouldNotAcquirePermitOnPoisonPill() throws Exception {
        var executorGate = new Semaphore(0);

        Executor blockingExecutor = command -> {
            try {
                executorGate.acquire();
                command.run();
            } catch (InterruptedException ignored) {
            }
        };

        var dispatcher = new Dispatcher<>(blockingExecutor, 1);

        dispatcher.start();
        dispatcher.stop();

        Semaphore limiter = extractSemaphore(dispatcher);

        assertThat(limiter.tryAcquire(100, TimeUnit.MILLISECONDS))
          .as("no permit should be leaked on stop")
          .isTrue();
    }

    private static Semaphore extractSemaphore(Dispatcher<?> dispatcher) throws Exception {
        Field limiterField = Dispatcher.class.getDeclaredField("limiter");
        limiterField.setAccessible(true);
        return (Semaphore) limiterField.get(dispatcher);
    }
}
