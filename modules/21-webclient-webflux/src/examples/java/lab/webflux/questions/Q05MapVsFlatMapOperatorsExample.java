package lab.webflux.questions;

import reactor.core.publisher.Mono;

public class Q05MapVsFlatMapOperatorsExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // .map() transforms T -> V synchronously (1-to-1 synchronous value mapping).
        // .flatMap() transforms T -> Mono<V> / Publisher<V> asynchronously (flattens nested
        // publishers).
        Mono<String> input = Mono.just("user-1");

        // map takes a synchronous function and wraps result in Mono<Integer>
        Mono<Integer> mapped = input.map(String::length);
        Integer length = mapped.block(); // 6

        // flatMap takes a function returning Mono<String> and flattens it to Mono<String>
        Mono<String> flatMapped = input.flatMap(id -> Mono.just("PROFILE:" + id));
        String profile = flatMapped.block(); // "PROFILE:user-1"

        System.out.println("Mapped length: " + length);
        System.out.println("FlatMapped profile: " + profile);
    }
}
