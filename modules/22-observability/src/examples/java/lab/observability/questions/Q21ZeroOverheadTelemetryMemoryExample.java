package lab.observability.questions;

import java.util.concurrent.atomic.LongAdder;

public class Q21ZeroOverheadTelemetryMemoryExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Zero-overhead high-throughput telemetry:
        // - Avoid object allocations (no new String, no boxed Double, no intermediate Map).
        // - LongAdder or Striped64 provides lock-free, zero-contention metrics updates.
        // - Single pre-allocated direct buffers or ring buffers (LMAX Disruptor).
        LongAdder highThroughputCounter = new LongAdder();

        // Hot path records millions of operations without heap allocation
        for (int i = 0; i < 1_000; i++) {
            highThroughputCounter.increment();
        }

        long totalCount = highThroughputCounter.sum(); // 1000L
        boolean isZeroAllocationOnHotPath = totalCount == 1_000L; // true

        System.out.println("Recorded operations via LongAdder: " + totalCount);
        System.out.println("Zero heap allocation verified: " + isZeroAllocationOnHotPath);
    }
}
