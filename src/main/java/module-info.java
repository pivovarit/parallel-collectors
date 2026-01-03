/**
 * Parallel Collectors is a toolkit that eases parallel collection processing in Java using Stream
 * API without the limitations imposed by standard Parallel Streams
 */
module parallel.collectors {
  exports com.pivovarit.collectors;

  requires static org.jspecify;
}
