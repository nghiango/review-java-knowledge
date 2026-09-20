package lab.webflux.questions;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Q12FlatMapConcurrencyLimitingExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // By default, Flux.flatMap(fn) uses Queues.SMALL_BUFFER_SIZE (256 concurrent inner
        // publishers).
        // Supplying the concurrency parameter: flatMap(fn, maxConcurrency) restricts concurrent
        // in-flight executions.
        int defaultConcurrency = 256; // 256
        int explicitConcurrency = 16; // 16

        Flux<Integer> pipeline =
                Flux.range(1, 100).flatMap(i -> Mono.just(i * 2), explicitConcurrency);

        Long count = pipeline.count().block(); // 100L
        boolean isBounded = explicitConcurrency < defaultConcurrency; // true

        System.out.println("Default flatMap concurrency: " + defaultConcurrency);
        System.out.println("Explicitly bounded flatMap concurrency: " + explicitConcurrency);
    }
}
