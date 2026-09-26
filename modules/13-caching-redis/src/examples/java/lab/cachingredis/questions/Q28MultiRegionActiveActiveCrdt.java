package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q28: How is Multi-Region Active-Active Redis replication designed, and how are write conflicts resolved?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q28MultiRegionActiveActiveCrdt {

    public static void main(String[] args) {
        // Standard Redis Replication vs Multi-Region Active-Active:
        // Standard Redis Sentinel/Cluster is Active-Passive (writes go only to primary in one region).
        // Cross-region Active-Active allows concurrent writes to local Redis clusters in us-east and eu-west.

        // Conflict Resolution Strategies:
        // 1. Last-Write-Wins (LWW): Based on high-precision NTP / Lamport timestamps.
        //    Trade-off: Clock skew between regions can cause newer writes to be silently discarded.
        // 2. Conflict-Free Replicated Data Types (CRDTs):
        //    Data structures mathematically designed to merge concurrent mutations deterministically
        //    without centralized coordination (e.g., PN-Counter, Observed-Removed Set / ORSet).

        Map<String, String> conflictResolution =
                Map.of(
                        "Counters", "PN-Counter (sum of local positive and negative increments)",
                        "Registers", "Last-Write-Wins (LWW) with hybrid logical clocks",
                        "Sets", "Add-Wins Observed-Removed Set (OR-Set)");

        boolean crdtEnablesEventualConsistencyWithoutLocks = true; // true
        boolean lwwSensitiveToClockDrift =
                conflictResolution.get("Registers").contains("Last-Write-Wins"); // true
    }
}
