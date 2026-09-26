package lab.webflux.questions;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Demonstrates a production incident where blocking operations inside a WebFilter or reactive pipeline
 * starved the Netty event loop / carrier thread, pinning workers and halting non-blocking HTTP processing.
 *
 * <p>Fix: Never perform blocking calls (e.g. legacy JDBC, synchronous REST, heavy cryptographic computation)
 * on Netty event loops (`reactor-http-epoll` or `reactor-http-nio`). Offload them via {@code publishOn(Schedulers.boundedElastic())}.</p>
 */
public class Q30IncidentReactiveWebFilterCarrierPinningExample {

    public static void main(String[] args) {
        AtomicBoolean badFilterPinningDetected = new AtomicBoolean(false);
        AtomicBoolean goodFilterOffloaded = new AtomicBoolean(false);

        // Anti-pattern: Blocking call directly inside reactive chain on event loop
        Mono<String> buggyFilterExecution = Mono.defer(() -> {
            String currentThread = Thread.currentThread().getName();
            // Simulating blocking token verification or legacy cache access
            if (currentThread.contains("main") || currentThread.contains("parallel") || currentThread.contains("reactor-http")) {
                badFilterPinningDetected.set(true);
            }
            return Mono.just("authenticated-user");
        });

        // Correct pattern: Explicitly offload blocking operations to boundedElastic scheduler
        Mono<String> safeFilterExecution = Mono.fromCallable(() -> {
            String currentThread = Thread.currentThread().getName();
            if (currentThread.contains("boundedElastic")) {
                goodFilterOffloaded.set(true);
            }
            return "authenticated-user-offloaded";
        }).subscribeOn(Schedulers.boundedElastic());

        buggyFilterExecution.block();
        safeFilterExecution.block();

        System.out.println("Bad filter executed on caller/event loop: " + badFilterPinningDetected.get()); // true
        System.out.println("Safe filter offloaded to boundedElastic: " + goodFilterOffloaded.get()); // true
    }
}
