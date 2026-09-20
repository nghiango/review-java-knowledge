package lab.performance.broken.contention;

import java.util.HashMap;
import java.util.Map;

public final class MetricAccumulator {
    private final Map<String, Long> totals = new HashMap<>();

    public synchronized void add(String metric, long delta) {
        totals.put(metric, totals.getOrDefault(metric, 0L) + delta);
    }

    public synchronized long total(String metric) {
        return totals.getOrDefault(metric, 0L);
    }
}
