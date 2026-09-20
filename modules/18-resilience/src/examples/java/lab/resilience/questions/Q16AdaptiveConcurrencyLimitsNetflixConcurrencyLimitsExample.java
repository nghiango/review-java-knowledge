package lab.resilience.questions;

public class Q16AdaptiveConcurrencyLimitsNetflixConcurrencyLimitsExample {

    record VegasLimiter(int currentLimit, double rttNoLoadMs, double currentRttMs) {
        int computeNewLimit() {
            // TCP Vegas gradient algorithm: adjust limit based on queueing delay
            double gradient = rttNoLoadMs / currentRttMs;
            if (gradient < 0.8) {
                // High queueing delay -> decrease concurrency limit
                return Math.max(1, (int) (currentLimit * gradient));
            } else {
                // Low latency -> incrementally increase limit
                return currentLimit + 1;
            }
        }
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // When downstream queues build up, RTT triples from 20ms to 60ms
        VegasLimiter limiter = new VegasLimiter(100, 20.0, 60.0);
        int adjustedLimit = limiter.computeNewLimit(); // 33

        boolean dynamicallyBackingOff = adjustedLimit < 100; // true
        System.out.println(
                "Limit automatically reduced from 100 to "
                        + adjustedLimit
                        + ": "
                        + dynamicallyBackingOff);
    }
}
