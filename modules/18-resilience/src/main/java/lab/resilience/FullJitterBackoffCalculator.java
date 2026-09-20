package lab.resilience;

import java.util.Random;

public class FullJitterBackoffCalculator {

    private final long baseIntervalMs;
    private final long maxIntervalMs;
    private final double multiplier;
    private final Random random;

    public FullJitterBackoffCalculator(long baseIntervalMs, long maxIntervalMs, double multiplier) {
        this.baseIntervalMs = baseIntervalMs;
        this.maxIntervalMs = maxIntervalMs;
        this.multiplier = multiplier;
        this.random = new Random();
    }

    public long calculateDelay(int attempt) {
        long ceiling =
                (long)
                        Math.min(
                                maxIntervalMs,
                                baseIntervalMs * Math.pow(multiplier, Math.max(0, attempt - 1)));
        return (long) (random.nextDouble() * ceiling);
    }
}
