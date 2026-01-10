package com.pivovarit.collectors;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class ForComprehension {

    private ForComprehension() {
    }

    public static <T1, T2> For2Optional<T1, T2> forc(
      Optional<T1> o1,
      Optional<T2> o2
    ) {
        Objects.requireNonNull(o1, "o1 is null");
        Objects.requireNonNull(o2, "o2 is null");
        return new For2Optional<>(o1, o2);
    }

    public static final class For2Optional<T1, T2> {

        private final Optional<T1> o1;
        private final Optional<T2> o2;

        private For2Optional(Optional<T1> o1, Optional<T2> o2) {
            this.o1 = o1;
            this.o2 = o2;
        }

        public <R> Optional<R> yield(
          BiFunction<? super T1, ? super T2, ? extends R> f
        ) {
            Objects.requireNonNull(f, "f is null");

            return o1.flatMap(t1 ->
              o2.map(t2 -> f.apply(t1, t2)));
        }
    }

    public static <T1, T2> ForLazy2Optional<T1, T2> forc(
      Optional<T1> o1,
      Function<? super T1, Optional<T2>> o2
    ) {
        Objects.requireNonNull(o1, "o1 is null");
        Objects.requireNonNull(o2, "o2 is null");
        return new ForLazy2Optional<>(o1, o2);
    }

    public static final class ForLazy2Optional<T1, T2> {

        private final Optional<T1> o1;
        private final Function<? super T1, Optional<T2>> o2;

        private ForLazy2Optional(
          Optional<T1> o1,
          Function<? super T1, Optional<T2>> o2
        ) {
            this.o1 = o1;
            this.o2 = o2;
        }

        /**
         * Binds the first Optional, then derives the second from it.
         * If either step is empty, the result is empty.
         */
        public <R> Optional<R> yield(
          BiFunction<? super T1, ? super T2, ? extends R> f
        ) {
            Objects.requireNonNull(f, "f is null");

            return o1.flatMap(t1 ->
              o2.apply(t1)
                .map(t2 -> f.apply(t1, t2)));
        }
    }

    public static <T1, T2> For2Stream<T1, T2> forc(
      Stream<T1> s1,
      Stream<T2> s2
    ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        return new For2Stream<>(s1, s2);
    }

    public static final class For2Stream<T1, T2> {
        private final Stream<T1> s1;
        private final Stream<T2> s2;

        private For2Stream(Stream<T1> s1, Stream<T2> s2) {
            this.s1 = s1;
            this.s2 = s2;
        }

        public <R> Stream<R> yield(
          BiFunction<? super T1, ? super T2, ? extends R> f
        ) {
            Objects.requireNonNull(f);

            List<T2> s2evaluated = s2.toList();

            return s1.flatMap(t1 ->
              s2evaluated.stream().map(t2 -> f.apply(t1, t2)));
        }
    }

    public static <T1, T2> ForLazy2Stream<T1, T2> forc(
      Stream<T1> s1,
      Function<? super T1, Stream<T2>> s2
    ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        return new ForLazy2Stream<>(s1, s2);
    }

    public static final class ForLazy2Stream<T1, T2> {
        private final Stream<T1> s1;
        private final Function<? super T1, Stream<T2>> s2;

        private ForLazy2Stream(
          Stream<T1> s1,
          Function<? super T1, Stream<T2>> s2
        ) {
            this.s1 = s1;
            this.s2 = s2;
        }

        public <R> Stream<R> yield(
          BiFunction<? super T1, ? super T2, ? extends R> f
        ) {
            Objects.requireNonNull(f);

            return s1.flatMap(t1 ->
              s2.apply(t1)
                .map(t2 -> f.apply(t1, t2)));
        }
    }
}

