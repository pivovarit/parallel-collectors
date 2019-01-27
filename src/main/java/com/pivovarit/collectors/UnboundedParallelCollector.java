package com.pivovarit.collectors;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Grzegorz Piwowarek
 */
class UnboundedParallelCollector<T, R1, R2 extends Collection<R1>> extends AbstractParallelCollector<T, R1, R2> {
    UnboundedParallelCollector(
      Function<T, R1> operation,
      Supplier<R2> collection,
      Executor executor) {
        super(operation, collection, executor);
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }
}
