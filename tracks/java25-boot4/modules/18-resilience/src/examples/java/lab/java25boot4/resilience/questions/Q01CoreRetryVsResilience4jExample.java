package lab.java25boot4.resilience.questions;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import lab.java25boot4.resilience.ModernResilientExecutionEngine;

/**
 * Q01: How does modern retry execution compare between native functional backoff and legacy
 * external decorators?
 */
public class Q01CoreRetryVsResilience4jExample {

    public static void main(String[] args) {
        var retryConfig =
                new ModernResilientExecutionEngine.RetryConfig(
                        3,
                        Duration.ofMillis(10),
                        Duration.ofMillis(50),
                        t -> t instanceof IllegalStateException);
        var circuit =
                new ModernResilientExecutionEngine.LockFreeCircuitBreaker(
                        5, Duration.ofMillis(100));
        var engine = new ModernResilientExecutionEngine(retryConfig, circuit, 10);

        var attempts = new AtomicInteger(0);
        String result =
                engine.executeWithResilience(
                        () -> {
                            if (attempts.incrementAndGet() < 2) {
                                throw new IllegalStateException("Temporary blip");
                            }
                            return "SUCCESS";
                        });

        System.out.println("Result: " + result); // Result: SUCCESS
        System.out.println("Total attempts: " + attempts.get()); // Total attempts: 2
    }
}
