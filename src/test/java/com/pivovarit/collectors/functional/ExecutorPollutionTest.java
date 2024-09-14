package com.pivovarit.collectors.functional;

import com.pivovarit.collectors.ParallelCollectors;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.Batching.parallel;
import static java.util.stream.Collectors.toList;

@ExtendWith(ExecutorPollutionTest.ContextProvider.class)
class ExecutorPollutionTest {

    @TestTemplate
    void shouldNotPolluteExecutorWhenNoParallelism(CollectorFactory<Integer> collector) {
        try (var e = warmedUp(new ThreadPoolExecutor(1 , 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(2)))) {

            var result = Stream.generate(() -> 42)
              .limit(1000)
              .collect(collector.apply(i -> i, e, 1));

            switch (result) {
                case CompletableFuture<?> cf -> cf.join();
                case Stream<?> s -> s.forEach((__) -> {});
                default -> throw new IllegalStateException("can't happen");
            }
        }
    }

    @TestTemplate
    void shouldNotPolluteExecutorWhenLimitedParallelism(CollectorFactory<Integer> collector) {
        try (var e = warmedUp(new ThreadPoolExecutor(2 , 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(2)))) {

            var result = Stream.generate(() -> 42)
              .limit(1000)
              .collect(collector.apply(i -> i, e, 2));

            switch (result) {
                case CompletableFuture<?> cf -> cf.join();
                case Stream<?> s -> s.forEach((__) -> {});
                default -> throw new IllegalStateException("can't happen");
            }
        }
    }

    static class ContextProvider implements TestTemplateInvocationContextProvider {

        @Override
        public boolean supportsTestTemplate(ExtensionContext context) {
            return true;
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
            return Stream.of(
              collector("parallel()", (f, e, p) -> ParallelCollectors.parallel(f, e, p)),
              collector("parallel(toList())", (f, e, p) -> ParallelCollectors.parallel(f, toList(), e, p)),
              collector("parallelToStream()", (f, e, p) -> ParallelCollectors.parallelToStream(f, e, p)),
              collector("parallelToOrderedStream()", (f, e, p) -> ParallelCollectors.parallelToOrderedStream(f, e, p)),
              collector("parallel() (batching)", (f, e, p) -> parallel(f, e, p)),
              collector("parallel(toList()) (batching)", (f, e, p) -> parallel(f, toList(), e, p)),
              collector("parallelToStream() (batching)", (f, e, p) -> ParallelCollectors.Batching.parallelToStream(f, e, p)),
              collector("parallelToOrderedStream() (batching)", (f, e, p) -> ParallelCollectors.Batching.parallelToOrderedStream(f, e, p))
            );
        }
    }

    interface CollectorFactory<T> {
        Collector<T, ?, ?> apply(Function<T, ?> function, Executor executorService, int parallelism);
    }

    private static ThreadPoolExecutor warmedUp(ThreadPoolExecutor e) {
        for (int i = 0; i < e.getCorePoolSize(); i++) {
            e.submit(() -> {});
        }
        return e;
    }

    private static <T> TestTemplateInvocationContext collector(String name, CollectorFactory<T> factory) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return name + " [" + invocationIndex + "]";
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return List.of(new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
                        return parameterContext.getParameter().getType().equals(CollectorFactory.class);
                    }

                    @Override
                    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
                        return factory;
                    }
                });
            }
        };
    }
}
