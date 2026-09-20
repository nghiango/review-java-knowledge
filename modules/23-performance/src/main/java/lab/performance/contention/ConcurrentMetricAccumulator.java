package lab.performance.contention;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public final class ConcurrentMetricAccumulator {
    private final ConcurrentHashMap<String, LongAdder> totals = new ConcurrentHashMap<>();

    public void add(String metric, long delta) {
        totals.computeIfAbsent(metric, ignored -> new LongAdder()).add(delta);
    }

    public long total(String metric) {
        LongAdder total = totals.get(metric);
        // A weakly consistent telemetry snapshot is preferable to serializing every writer.
        return total == null ? 0 : total.sum();
    }
}
