package lab.concurrency.broken.lostupdate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UserVisitMetricsService {
    private final HitCounter hitCounter = new HitCounter();
    private final Map<String, Integer> visitsPerUser = new HashMap<>();
    private boolean active = true;

    public void recordUserHit(String userId) {
        if (!active) {
            return;
        }
        hitCounter.recordHit();

        Integer count = visitsPerUser.getOrDefault(userId, 0);
        visitsPerUser.put(userId, count + 1);
    }

    public synchronized void recordAuditVisit(String userId) {
        if (!active) {
            return;
        }
        try {
            // Simulated slow remote audit emission inside synchronized monitor
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        recordUserHit(userId);
    }

    public void shutdown() {
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public long getTotalHits() {
        return hitCounter.getTotalHits();
    }

    public Map<String, Integer> getVisitsPerUser() {
        return Collections.unmodifiableMap(visitsPerUser);
    }
}
