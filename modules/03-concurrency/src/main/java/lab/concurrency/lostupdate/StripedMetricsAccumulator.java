package lab.concurrency.lostupdate;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe metrics accumulator that stripes counter updates per user ID using {@link
 * ConcurrentHashMap} and atomic {@link LongAdder} values.
 */
public class StripedMetricsAccumulator {
    private final ConcurrentHashMap<String, LongAdder> userCounters = new ConcurrentHashMap<>();

    public void recordUserHit(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        userCounters.computeIfAbsent(userId, k -> new LongAdder()).increment();
    }

    public long getUserHits(String userId) {
        LongAdder adder = userCounters.get(userId);
        return adder == null ? 0L : adder.sum();
    }

    public Map<String, Long> snapshot() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        userCounters.forEach((k, v) -> result.put(k, v.sum()));
        return Collections.unmodifiableMap(result);
    }

    public void clear() {
        userCounters.clear();
    }
}
