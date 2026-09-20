package lab.webflux.questions;

import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.publisher.Mono;

public class Q04LazySubscriptionEvaluationExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // "Nothing happens until you subscribe":
        // Building a reactive pipeline only creates an execution assembly graph (cold publisher).
        // The supplied lambda is never invoked during declaration.
        AtomicBoolean invoked = new AtomicBoolean(false);

        Mono<String> coldPipeline =
                Mono.fromSupplier(
                        () -> {
                            invoked.set(true);
                            return "executed";
                        });

        boolean beforeSubscribe = invoked.get(); // false

        coldPipeline.subscribe();

        boolean afterSubscribe = invoked.get(); // true

        System.out.println("Invoked before subscribe: " + beforeSubscribe);
        System.out.println("Invoked after subscribe: " + afterSubscribe);
    }
}
