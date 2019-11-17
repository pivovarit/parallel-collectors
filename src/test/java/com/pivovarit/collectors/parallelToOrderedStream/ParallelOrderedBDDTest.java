package com.pivovarit.collectors.parallelToOrderedStream;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.pivovarit.collectors.ParallelCollectors.parallelToOrderedStream;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Grzegorz Piwowarek
 */
@RunWith(JUnitQuickcheck.class)
public class ParallelOrderedBDDTest extends ExecutorAwareTest {

    @Property(trials = 10)
    public void shouldCollectToListInEncounterOrder(@InRange(minInt = 3, maxInt = 10) int unitsOfWork, @InRange(minInt = 10, maxInt = 40) int parallelism) {
        // given
        executor = threadPoolExecutor(unitsOfWork);

        List<Integer> result = IntStream.range(0, unitsOfWork).boxed()
          .collect(parallelToOrderedStream(i -> returnWithDelay(i, ofMillis(new Random().nextInt(20))), executor, parallelism))
          .collect(Collectors.toList());

        assertThat(result).isSorted();

        executor.shutdownNow();
    }
}
