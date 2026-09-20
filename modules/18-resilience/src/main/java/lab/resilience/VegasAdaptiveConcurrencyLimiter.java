package lab.resilience;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class VegasAdaptiveConcurrencyLimiter {

    private final AtomicInteger currentLimit;
    private final Duration rttNoLoad;

    public VegasAdaptiveConcurrencyLimiter(int initialLimit, Duration rttNoLoad) {
        this.currentLimit = new AtomicInteger(initialLimit);
        this.rttNoLoad = rttNoLoad;
    }

    public int onSample(Duration observedRtt) {
        double gradient = (double) rttNoLoad.toNanos() / observedRtt.toNanos();
        int limit = currentLimit.get();

        int nextLimit;
        if (gradient < 0.8) {
            // High queueing delay -> back off concurrency
            nextLimit = Math.max(1, (int) (limit * gradient));
        } else {
            // Healthy latency -> gently probe higher capacity
            nextLimit = limit + 1;
        }

        currentLimit.set(nextLimit);
        return nextLimit;
    }

    public int getLimit() {
        return currentLimit.get();
    }
}
