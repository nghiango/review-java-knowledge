package lab.concurrency.lostupdate;

import java.util.concurrent.atomic.LongAdder;

/**
 * High-throughput thread-safe counter using {@link LongAdder}. Internally maintains a cell array
 * across contending threads to avoid CAS retries under high contention.
 */
public class AtomicHitCounter {
    private final LongAdder adder = new LongAdder();

    public void recordHit() {
        adder.increment();
    }

    public void addHits(long count) {
        if (count > 0) {
            adder.add(count);
        }
    }

    public long getTotalHits() {
        return adder.sum();
    }

    public void reset() {
        adder.reset();
    }
}
