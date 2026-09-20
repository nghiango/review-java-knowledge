package lab.webflux.questions;

import java.time.Duration;

public class Q20WebClientCircuitBreakerBulkheadExample {

    record ResilienceMeshConfig(
            Duration slidingWindow, int failureRateThreshold, int maxConcurrentCalls) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // In reactive WebClient architectures, Resilience4j integrates via Reactor operators:
        // - CircuitBreaker.transform(circuitBreaker): Opens circuit on high failure/slow call rate,
        // failing fast.
        // - Bulkhead.transform(bulkhead): Limits concurrent subscribers to prevent downstream
        // socket saturation.
        ResilienceMeshConfig config = new ResilienceMeshConfig(Duration.ofSeconds(10), 50, 20);

        int maxInFlight = config.maxConcurrentCalls(); // 20
        int threshold = config.failureRateThreshold(); // 50

        System.out.println("Reactive bulkhead concurrency ceiling: " + maxInFlight);
        System.out.println("Circuit breaker trip threshold percentage: " + threshold);
    }
}
