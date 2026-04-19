package com.pivovarit.collectors;

import java.util.concurrent.Executor;
import java.util.function.Function;

public class CollectingConfigurer {
    public CollectingConfigurer parallelism(int p) {
        return null;
    }

    public CollectingConfigurer executor(Executor e) {
        return null;
    }

    public CollectingConfigurer batching() {
        return null;
    }

    public CollectingConfigurer taskDecorator(Function<Runnable, Runnable> decorator) {
        return null;
    }

    public CollectingConfigurer executorDecorator(Function<Executor, Executor> o) {
        return null;
    }
}
