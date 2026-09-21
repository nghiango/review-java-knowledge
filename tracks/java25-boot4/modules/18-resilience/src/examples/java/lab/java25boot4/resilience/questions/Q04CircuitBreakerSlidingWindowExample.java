package lab.java25boot4.resilience.questions;

import java.time.Duration;
import lab.java25boot4.resilience.ModernResilientExecutionEngine;

/**
 * Q04: How does a lock-free circuit breaker track failures and transition across CLOSED, OPEN, and
 * HALF_OPEN?
 */
public class Q04CircuitBreakerSlidingWindowExample {

    public static void main(String[] args) {
        var circuit =
                new ModernResilientExecutionEngine.LockFreeCircuitBreaker(2, Duration.ofMillis(50));

        System.out.println("Initial state: " + circuit.getState()); // Initial state: CLOSED

        // Record two failures to trigger breaker
        try {
            circuit.execute(
                    () -> {
                        throw new RuntimeException("Failure 1");
                    });
        } catch (RuntimeException ignored) {
        }

        try {
            circuit.execute(
                    () -> {
                        throw new RuntimeException("Failure 2");
                    });
        } catch (RuntimeException ignored) {
        }

        System.out.println(
                "State after threshold: " + circuit.getState()); // State after threshold: OPEN
    }
}
