package lab.concurrency.lostupdate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe visit metrics service with lock-free atomic counters, volatile lifecycle state, and
 * decoupled fast non-blocking execution.
 */
public class UserVisitMetricsService {
    private final AtomicHitCounter globalCounter = new AtomicHitCounter();
    private final StripedMetricsAccumulator userMetrics = new StripedMetricsAccumulator();
    private final AtomicBoolean active = new AtomicBoolean(true);

    public boolean recordUserHit(String userId) {
        if (!active.get()) {
            return false;
        }
        globalCounter.recordHit();
        userMetrics.recordUserHit(userId);
        return true;
    }

    public void shutdown() {
        active.set(false);
    }

    public boolean isActive() {
        return active.get();
    }

    public long getTotalHits() {
        return globalCounter.getTotalHits();
    }

    public long getUserHits(String userId) {
        return userMetrics.getUserHits(userId);
    }

    public Map<String, Long> getMetricsSnapshot() {
        return userMetrics.snapshot();
    }
}
