package lab.resilience.questions;

import java.util.Random;

public class Q03ExponentialBackoffAndJitterStrategiesExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        long baseIntervalMs = 100L;
        int attempt = 3;
        double multiplier = 2.0;

        // Pure exponential backoff: base * (multiplier ^ (attempt - 1))
        long exponentialBackoff =
                (long) (baseIntervalMs * Math.pow(multiplier, attempt - 1)); // 400

        // Full Jitter: random between 0 and exponentialBackoff
        Random rng = new Random(42);
        long fullJitter = rng.nextLong(exponentialBackoff + 1);

        boolean isWithinFullJitterBounds =
                fullJitter >= 0 && fullJitter <= exponentialBackoff; // true

        System.out.println(
                "Exponential backoff: "
                        + exponentialBackoff
                        + "ms, Jitter within bounds: "
                        + isWithinFullJitterBounds);
    }
}
