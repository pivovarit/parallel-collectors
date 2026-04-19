package com.pivovarit.collectors;

import java.util.concurrent.Executor;
import java.util.function.Function;

public class StreamingConfigurer {
    public StreamingConfigurer parallelism(int p) {
        return null;
    }

    public StreamingConfigurer executor(Executor e) {
        return null;
    }

    public StreamingConfigurer batching() {
        return null;
    }

    public StreamingConfigurer ordered() {
        return null;
    }

    public StreamingConfigurer taskDecorator(Function<Runnable, Runnable> o) {
        return null;
    }

    public StreamingConfigurer executorDecorator(Function<Executor, Executor> o) {
        return null;
    }
}
