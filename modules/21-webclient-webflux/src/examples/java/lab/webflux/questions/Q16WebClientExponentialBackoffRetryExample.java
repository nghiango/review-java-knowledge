package lab.webflux.questions;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class Q16WebClientExponentialBackoffRetryExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Reactor provides Retry.backoff(maxAttempts, minBackoff).jitter(jitterFactor).
        // It recalculates delays exponentially: minBackoff * 2^(attempt-1) + jitter,
        // preventing retry storms against struggling downstream services.
        AtomicInteger attempts = new AtomicInteger(0);

        Mono<String> retriedMono =
                Mono.fromSupplier(
                                () -> {
                                    if (attempts.incrementAndGet() < 3) {
                                        throw new IllegalStateException("Transient gateway error");
                                    }
                                    return "recovered-success";
                                })
                        .retryWhen(Retry.backoff(3, Duration.ofMillis(10)));

        String finalResult = retriedMono.block(); // "recovered-success"
        int totalAttemptsMade = attempts.get(); // 3

        System.out.println("Final execution result: " + finalResult);
        System.out.println("Total attempts made before success: " + totalAttemptsMade);
    }
}
