package com.pivovarit.collectors;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.pivovarit.collectors.infrastructure.ExecutorAwareTest;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.parallel;
import static com.pivovarit.collectors.ParallelCollectors.parallelToOrderedStream;
import static com.pivovarit.collectors.ParallelCollectors.parallelToStream;
import static com.pivovarit.collectors.infrastructure.TestUtils.TRIALS;
import static com.pivovarit.collectors.infrastructure.TestUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;

/**
 * @author Grzegorz Piwowarek
 */
@RunWith(JUnitQuickcheck.class)
public class OrderingTest extends ExecutorAwareTest {

    @Property(trials = 10)
    public void shouldCollectInEncounterOrder(@InRange(minInt = 3, maxInt = 10) int unitsOfWork, @InRange(minInt = 10, maxInt = 40) int parallelism) {
        // given
        executor = threadPoolExecutor(unitsOfWork);

        List<Integer> result = IntStream.range(0, unitsOfWork).boxed()
          .collect(parallelToOrderedStream(i -> returnWithDelay(i, ofMillis(new Random()
            .nextInt(20))), executor, parallelism))
          .collect(toList());

        assertThat(result).isSorted();
    }

    @Property(trials = TRIALS)
    public void shouldCollectInCompletionOrder() {
        // given
        executor = threadPoolExecutor(4);

        List<Integer> result = Stream.of(350, 200, 0, 400)
          .collect(parallelToStream(i -> returnWithDelay(i, ofMillis(i)), executor, 4))
          .limit(2)
          .collect(toList());

        assertThat(result).isSorted();
    }

    @Property(trials = TRIALS)
    public void shouldCollectToListInEncounterOrder() {
        // given
        executor = threadPoolExecutor(4);

        Integer[] seq = {350, 200, 0, 400};

        List<Integer> result = Stream.of(seq)
          .collect(parallel(i -> returnWithDelay(i, ofMillis(i)), toList(), executor, 4)).join();

        assertThat(result).containsExactly(seq);
    }
}
