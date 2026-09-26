package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q27: How is memory fragmentation ratio diagnosed in Redis, and how does active defragmentation work?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q27MemoryFragmentationRatioActiveDefrag {

    public static void main(String[] args) {
        // Diagnosis via INFO MEMORY:
        // mem_fragmentation_ratio = used_memory_rss / used_memory
        // used_memory_rss: OS allocated Resident Set Size (physical RAM used by Redis process)
        // used_memory: Total bytes allocated by Redis memory allocator (jemalloc) for stored keys

        double usedMemoryMb = 1000.0;
        double usedMemoryRssMb = 1800.0;
        double memFragmentationRatio = usedMemoryRssMb / usedMemoryMb; // 1.8

        // Interpretation thresholds:
        // ratio > 1.5: High fragmentation (>50% memory wasted due to jemalloc allocation holes)
        // 1.0 <= ratio <= 1.5: Healthy normal range
        // ratio < 1.0: Redis memory has been swapped out to disk by OS (fatal latency penalty!)
        boolean isHighFragmentation = memFragmentationRatio > 1.5; // true
        boolean isSwappingToDisk = memFragmentationRatio < 1.0; // false

        // Remediation:
        // In Redis 4.0+, enable online active defragmentation without restarting Redis:
        // CONFIG SET activedefrag yes
        // active-defrag-ignore-bytes 100mb
        // active-defrag-threshold-lower 10 (percentage)
        Map<String, String> tuning =
                Map.of(
                        "activedefrag", "yes",
                        "active-defrag-threshold-lower", "10",
                        "active-defrag-cycle-max", "75");

        boolean onlineDefragSupportedWithoutRestart =
                tuning.get("activedefrag").equals("yes"); // true
    }
}
