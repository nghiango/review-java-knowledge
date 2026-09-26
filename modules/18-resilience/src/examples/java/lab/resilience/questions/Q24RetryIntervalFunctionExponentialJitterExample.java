package lab.resilience.questions;

import java.util.Map;

/**
 * Q24: How does Resilience4j's IntervalFunction implement exponential backoff with randomized jitter?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q24RetryIntervalFunctionExponentialJitterExample {

    public static void main(String[] args) {
        // Resilience4j IntervalFunction provides built-in exponential backoff:
        // IntervalFunction.ofExponentialRandomBackoff(initialInterval, multiplier, randomizationFactor)
        // Formula: interval = initialInterval * (multiplier ^ (attempt - 1))
        // Jitter window: [interval * (1 - randomizationFactor), interval * (1 + randomizationFactor)]

        long initialIntervalMs = 100;
        double multiplier = 2.0;
        double randomizationFactor = 0.5; // 50% jitter

        long attempt1Base = (long) (initialIntervalMs * Math.pow(multiplier, 0)); // 100ms
        long attempt2Base = (long) (initialIntervalMs * Math.pow(multiplier, 1)); // 200ms
        long attempt3Base = (long) (initialIntervalMs * Math.pow(multiplier, 2)); // 400ms

        boolean backoffGrowsExponentially = attempt3Base > attempt2Base && attempt2Base > attempt1Base; // true

        Map<String, String> jitterComparison =
                Map.of(
                        "No Jitter", "Synchronized retry bursts; hammers recovering downstream service",
                        "Exponential Random Jitter", "Spreads retry waves uniformly across time window");

        boolean preventsThunderingHerd =
                jitterComparison.get("Exponential Random Jitter").contains("Spreads retry waves"); // true
    }
}
