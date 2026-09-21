package lab.java25boot4.resilience.questions;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import lab.java25boot4.resilience.ModernResilientExecutionEngine;

/**
 * Q13: In an incident where downstream payment gateway latency spikes to 30s, how does a modern
 * circuit breaker prevent cascading thread starvation?
 */
public class Q13ScenarioCascadingPaymentOutageExample {

    public static void main(String[] args) {
        var retryConfig =
                new ModernResilientExecutionEngine.RetryConfig(
                        1, Duration.ofMillis(1), Duration.ofMillis(1), t -> false);
        var circuitBreaker =
                new ModernResilientExecutionEngine.LockFreeCircuitBreaker(
                        3, Duration.ofSeconds(10));
        var engine = new ModernResilientExecutionEngine(retryConfig, circuitBreaker, 10);

        var executedAttempts = new AtomicInteger(0);

        // 3 calls fail, tripping the breaker
        for (int i = 0; i < 3; i++) {
            try {
                engine.executeWithResilience(
                        () -> {
                            executedAttempts.incrementAndGet();
                            throw new RuntimeException("504 Gateway Timeout");
                        });
            } catch (Exception ignored) {
            }
        }

        System.out.println(
                "Circuit state after failures: "
                        + engine.getCircuitState()); // Circuit state after failures: OPEN

        // 4th call: fails fast immediately, downstream is NOT called
        boolean fastFailed = false;
        try {
            engine.executeWithResilience(
                    () -> {
                        executedAttempts.incrementAndGet();
                        return "SUCCESS";
                    });
        } catch (ModernResilientExecutionEngine.CircuitBreakerOpenException expected) {
            fastFailed = true;
        }

        System.out.println("Fast-fail occurred: " + fastFailed); // Fast-fail occurred: true
        System.out.println(
                "Downstream protected calls count: "
                        + executedAttempts.get()); // Downstream protected calls count: 3
    }
}
