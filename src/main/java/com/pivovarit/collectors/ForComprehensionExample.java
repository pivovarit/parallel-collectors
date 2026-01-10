package com.pivovarit.collectors;

import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.pivovarit.collectors.ForComprehension.forc;

class ForComprehensionExample {

    record StreamExample() {
        public static void main(String[] args) {
            Stream<Integer> xs = Stream.of(1, 2);
            Stream<Integer> ys = Stream.of(10, 20);

            forc(xs, Stream.of(10, 20))
              .yield(Map::entry)
              .forEach(System.out::println);
        }
    }

    record StreamLazyExample() {
        public static void main(String[] args) {
            Stream<Integer> xs = Stream.of(1, 2, 3);

            forc(xs, x -> IntStream.range(0, x).boxed())
              .yield((x, y) -> x + ":" + y)
              .forEach(System.out::println);
        }
    }

    record EagerExample() {
        public static void main(String[] args) {
            Optional<Integer> width = Optional.of(3);
            Optional<Integer> height = Optional.of(4);

            var result = forc(
              width,
              height
            ).yield((w, h) -> w * h)
              .orElse(-1);

            System.out.println("result = " + result);
        }
    }

    record LazyExample() {
        public static void main(String[] args) {
            Optional<String> city = forc(
              findUserById(42),
              id -> findAddress(id)
            ).yield((u, a) -> u.name() + " lives in " + a.city());

            System.out.println(city); // Optional[Ada lives in London]
        }

        public static Optional<User> findUserById(long id) {
            return id == 42 ? Optional.of(new User(42, "Ada")) : Optional.empty();
        }

        public static Optional<Address> findAddress(User user) {
            return user.id() == 42 ? Optional.of(new Address("London")) : Optional.empty();
        }

        record User(long id, String name) {
        }

        record Address(String city) {
        }
    }
}
