package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q12: How are Hot Keys detected in Redis, and what architectural designs protect single Redis
 * nodes from hot-key saturation?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q12HotKeyDetectionAndHandling {

    public static void main(String[] args) {
        // Hot Key Problem: A single key (e.g. flash sale item or viral tweet) receives tens of
        // thousands
        // of requests per second. Since a key lives on one Redis shard, that shard's single thread
        // and NIC saturate.
        Map<String, String> detectionTools =
                Map.of(
                        "redis-cli --hotkeys",
                                "Scans keyspace sampling maxmemory-policy LFU counters to find hot keys",
                        "CLIENT PAUSE / MONITOR",
                                "Real-time command stream inspection (caution: high performance overhead)",
                        "Client-side Metrics",
                                "Micrometer timer tracking high query volume keys at application boundary");

        Map<String, String> remediationSolutions =
                Map.of(
                        "L1 Local In-Memory Cache",
                                "Cache hot key in local Caffeine memory for 5–10 seconds, shielding Redis",
                        "Key Sharding / Replication",
                                "Replicate key with random suffixes (e.g. item:101_#1, item:101_#2)",
                        "Read Replicas",
                                "Route read-only queries across multiple Redis read replicas via load balancer");

        boolean hotkeysCliUsesLfu =
                detectionTools.get("redis-cli --hotkeys").contains("LFU"); // true
        boolean l1CacheShieldsRedis =
                remediationSolutions
                        .get("L1 Local In-Memory Cache")
                        .contains("shielding Redis"); // true
    }
}
