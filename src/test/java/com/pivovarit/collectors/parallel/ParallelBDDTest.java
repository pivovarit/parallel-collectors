package com.pivovarit.collectors.parallel;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallel;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Grzegorz Piwowarek
 */
@RunWith(JUnitQuickcheck.class)
public class ParallelBDDTest extends ExecutorAwareTest {

    @Property(trials = 5)
    public void shouldCollectToListInCompletionOrder() {
        // given
        executor = threadPoolExecutor(10);

        List<Integer> result = Stream.of(350, 200, 0, 500)
          .collect(parallel(i -> returnWithDelay(i, ofMillis(i)), executor, 4))
          .collect(Collectors.toList());

        assertThat(result).isSorted();

        executor.shutdownNow();
    }
}
