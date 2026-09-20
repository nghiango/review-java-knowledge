package lab.kafka.questions;

import java.util.Map;

/**
 * Q21: What are the architectural trade-offs among RDBMS unique constraints, Redis SETNX, and Bloom
 * filters for idempotent consumer deduplication?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q21MessageDeduplicationStoreDesigns {

    public static void main(String[] args) {
        // Trade-offs across deduplication storage strategies:
        Map<String, String> strategies =
                Map.of(
                        "RDBMS Unique Constraint",
                                "ACID guarantees with business mutation in single transaction; higher DB IOPS write load",
                        "Redis SETNX with TTL",
                                "Sub-millisecond latency and automatic expiry; risk of stale state or double write if DB fails after Redis check",
                        "Sliding Window Bloom Filter",
                                "Minimal memory footprint for billions of IDs; small false positive probability (potential dropped events)");

        boolean rdbmsProvidesAcid =
                strategies.get("RDBMS Unique Constraint").contains("single transaction"); // true

        boolean redisProvidesTtl =
                strategies.get("Redis SETNX with TTL").contains("automatic expiry"); // true
    }
}
