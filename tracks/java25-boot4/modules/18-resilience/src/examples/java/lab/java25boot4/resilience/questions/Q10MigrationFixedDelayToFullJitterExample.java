package lab.java25boot4.resilience.questions;

import java.time.Duration;
import lab.java25boot4.resilience.ModernResilientExecutionEngine;

/**
 * Q10: How does replacing fixed-delay retries with Full Jitter prevent the thundering herd problem
 * during service recovery?
 */
public class Q10MigrationFixedDelayToFullJitterExample {

    public static void main(String[] args) {
        long fixedDelayMillis = 1000L;
        Duration base = Duration.ofMillis(100);
        Duration max = Duration.ofMillis(1000);

        // Under fixed delay, 100 concurrent threads all retry at t=1000ms simultaneously
        // Under full jitter, retries are uniformly distributed across [0, 2^attempt * base]
        long jitteredAttempt1 =
                ModernResilientExecutionEngine.calculateJitteredBackoff(1, base, max);
        long jitteredAttempt2 =
                ModernResilientExecutionEngine.calculateJitteredBackoff(2, base, max);

        boolean dispersed = jitteredAttempt1 <= 100 && jitteredAttempt2 <= 200;

        System.out.println(
                "Fixed delay constant: " + fixedDelayMillis); // Fixed delay constant: 1000
        System.out.println(
                "Jitter delays dispersed: " + dispersed); // Jitter delays dispersed: true
    }
}
