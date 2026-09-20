package lab.webflux.questions;

import reactor.core.publisher.Mono;

public class Q13ReactiveErrorHandlingStrategiesExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // In Reactive Streams, an onError signal is terminal: it halts and tears down the stream.
        // Reactive operators allow error recovery:
        // - onErrorReturn(fallbackValue): Emits fallback value when error occurs.
        // - onErrorResume(fallbackPublisher): Switches to an alternative fallback Publisher.
        // - onErrorMap(mapper): Translates low-level exception into business domain exception.

        Mono<String> withReturn =
                Mono.<String>error(new RuntimeException("Downstream timeout"))
                        .onErrorReturn("cached-fallback");

        Mono<String> withResume =
                Mono.<String>error(new RuntimeException("Primary failed"))
                        .onErrorResume(e -> Mono.just("secondary-service-result"));

        String resultReturn = withReturn.block(); // "cached-fallback"
        String resultResume = withResume.block(); // "secondary-service-result"

        System.out.println("Result with onErrorReturn: " + resultReturn);
        System.out.println("Result with onErrorResume: " + resultResume);
    }
}
