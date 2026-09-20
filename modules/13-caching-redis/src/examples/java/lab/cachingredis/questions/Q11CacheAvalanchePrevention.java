package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q11: What is a Cache Avalanche, and how do clustered architecture and jittered TTL prevent
 * cascading failures?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q11CacheAvalanchePrevention {

    public static void main(String[] args) {
        // Cache Avalanche:
        // Either (1) a massive batch of keys expires simultaneously, or (2) the Redis cluster
        // instance crashes.
        // In both cases, huge spikes of read traffic cascade directly into the database, causing DB
        // downtime.
        Map<String, String> countermeasures =
                Map.of(
                        "TTL Random Jitter",
                                "Spread key expiration across a wider time window (+/- 10-20%)",
                        "High Availability Clustering",
                                "Redis Sentinel or Redis Cluster with automatic master-replica failover",
                        "Circuit Breaking / Rate Limiting",
                                "Resilience4j circuit breaker rejects excess queries when DB latency spikes",
                        "Multi-Tier Caching (L1/L2)",
                                "Local Caffeine cache absorbs traffic even if distributed Redis is unreachable");

        boolean jitterSpreadsExpiration =
                countermeasures.get("TTL Random Jitter").contains("Spread key expiration"); // true
        boolean multiTierBuffersOutage =
                countermeasures
                        .get("Multi-Tier Caching (L1/L2)")
                        .contains("absorbs traffic"); // true
    }
}
