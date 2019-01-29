package com.pivovarit.collectors;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ParallelCollectors.inParallelToCollection;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToList;
import static com.pivovarit.collectors.ParallelCollectors.inParallelToSet;
import static com.pivovarit.collectors.ParallelCollectors.supplier;
import static com.pivovarit.collectors.TimeUtils.returnWithDelay;
import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertTimeout;

/**
 * @author Grzegorz Piwowarek
 */
class NonblockingCollectTest {

    @Test
    void shouldReturnImmediatelyCollection() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToCollection(ArrayList::new, Runnable::run, 42)));
    }

    @Test
    void shouldReturnImmediatelyCollectionUnbounded() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToCollection(ArrayList::new, Runnable::run)));
    }

    @Test
    void shouldReturnImmediatelyList() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToList(Runnable::run, 42)));
    }

    @Test
    void shouldReturnImmediatelyListUnbounded() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToList(Runnable::run)));
    }

    @Test
    void shouldReturnImmediatelySet() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToSet(Runnable::run, 42)));
    }

    @Test
    void shouldReturnImmediatelySetUnbounded() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE))))
            .limit(5)
            .collect(inParallelToSet(Runnable::run)));
    }

    @Test
    void shouldReturnImmediatelyCollectionMapping() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(inParallelToCollection(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), ArrayList::new, Runnable::run, 42)));
    }

    @Test
    void shouldReturnImmediatelyCollectionMappingUnbounded() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(inParallelToCollection(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), ArrayList::new, Runnable::run)));
    }

    @Test
    void shouldReturnImmediatelyListMapping() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(inParallelToList(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), Runnable::run, 42)));
    }

    @Test
    void shouldReturnImmediatelyListMappingUnbounded() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(inParallelToList(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), Runnable::run)));
    }

    @Test
    void shouldReturnImmediatelySetMapping() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(inParallelToSet(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), Runnable::run, 42)));
    }

    @Test
    void shouldReturnImmediatelySetMappingUnbounded() {
        assertTimeout(ofMillis(100), () ->
          Stream.generate(() -> supplier(() -> 42))
            .limit(5)
            .collect(inParallelToSet(i -> returnWithDelay(42L, ofMillis(Integer.MAX_VALUE)), Runnable::run)));
    }
}
