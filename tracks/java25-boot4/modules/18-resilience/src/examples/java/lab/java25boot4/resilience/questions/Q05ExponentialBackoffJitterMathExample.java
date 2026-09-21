package lab.java25boot4.resilience.questions;

import java.time.Duration;
import lab.java25boot4.resilience.ModernResilientExecutionEngine;

/**
 * Q05: What is the mathematical formulation of Full Jitter vs truncated exponential backoff in
 * high-concurrency systems?
 */
public class Q05ExponentialBackoffJitterMathExample {

    public static void main(String[] args) {
        Duration base = Duration.ofMillis(100);
        Duration max = Duration.ofMillis(1600);

        // Calculate jittered delays for attempts 1, 2, 3
        long delay1 = ModernResilientExecutionEngine.calculateJitteredBackoff(1, base, max);
        long delay2 = ModernResilientExecutionEngine.calculateJitteredBackoff(2, base, max);
        long delay3 = ModernResilientExecutionEngine.calculateJitteredBackoff(3, base, max);

        System.out.println(
                "Attempt 1 delay in bounds: "
                        + (delay1 >= 0 && delay1 <= 100)); // Attempt 1 delay in bounds: true
        System.out.println(
                "Attempt 2 delay in bounds: "
                        + (delay2 >= 0 && delay2 <= 200)); // Attempt 2 delay in bounds: true
        System.out.println(
                "Attempt 3 delay in bounds: "
                        + (delay3 >= 0 && delay3 <= 400)); // Attempt 3 delay in bounds: true
    }
}
