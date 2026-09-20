package lab.webflux.questions;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Q03MonoVsFluxSemanticsExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Mono represents an asynchronous sequence of 0 or 1 element (analogous to Optional or
        // CompletableFuture).
        // Flux represents an asynchronous sequence of 0 to N elements (analogous to Stream or
        // Iterable).
        Mono<String> singleItem = Mono.just("order-101");
        Flux<Integer> streamItems = Flux.just(10, 20, 30);

        String monoValue = singleItem.block(); // "order-101"
        Long fluxCount = streamItems.count().block(); // 3L

        System.out.println("Mono emitted: " + monoValue);
        System.out.println("Flux count: " + fluxCount);
    }
}
