package lab.webflux.questions;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public class Q15ReactorContextPropagationExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Because reactive streams switch execution threads freely between event loops and
        // schedulers,
        // ThreadLocal cannot be used reliably.
        // Project Reactor provides Reactor Context (an immutable map tied to the Subscriber).
        // It flows upstream from the subscriber towards the publishers.
        String contextKey = "X-Correlation-Id";

        Mono<String> pipeline =
                Mono.deferContextual(
                                ctx -> {
                                    String correlationId = ctx.getOrDefault(contextKey, "none");
                                    return Mono.just("trace:" + correlationId);
                                })
                        .contextWrite(Context.of(contextKey, "req-999"));

        String result = pipeline.block(); // "trace:req-999"
        boolean propagatesContext = result.contains("req-999"); // true

        System.out.println("Propagated context correlation ID: " + result);
    }
}
