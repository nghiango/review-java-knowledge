package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q17: How is a Two-Tier Cache (L1 Caffeine + L2 Redis) architected, and how is cross-node
 * invalidation handled?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q17TwoTierCachingL1L2 {

    public static void main(String[] args) {
        // Two-Tier Architecture:
        // L1: In-process Caffeine cache on each application instance.
        //     Latency: < 100 nanoseconds. Protects Redis and network NIC from saturation.
        // L2: Centralized Redis cluster.
        //     Latency: ~ 1 millisecond. Consistent data shared across all cluster instances.
        Map<String, String> layers =
                Map.of(
                        "L1", "Local Caffeine (nanoseconds, zero serialization, per-node heap)",
                        "L2",
                                "Distributed Redis (milliseconds, centralized consistency, persistent)");

        boolean l1ProvidesNanosecondLatency = layers.get("L1").contains("nanoseconds"); // true
        boolean l2ProvidesCentralizedStore = layers.get("L2").contains("centralized"); // true

        // Invalidation Problem:
        // When Node A mutates data, it evicts its own L1 and the central L2.
        // How does Node B know its local L1 has become stale?
        // Solution: Redis Pub/Sub or Redis Streams invalidation topic.
        // Node A publishes an eviction message; all peer instances subscribe and evict their local
        // L1.
        boolean pubSubCoordinatesL1Invalidation = true; // true
    }
}
