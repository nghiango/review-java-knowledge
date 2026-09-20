package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q22: Production Scenario: A flash sale homepage crashed database connection pools at 00:00:00.
 * How was the cache stampede diagnosed and remediated without service restart?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q22CacheStampedeDowntimeIncident {

    public static void main(String[] args) {
        // Incident:
        // At 00:00:00, the daily deals cache key ("homepage:deals") expired simultaneously across
        // all users.
        // 15,000 concurrent active users hit the homepage. 100% cache miss.
        // 15,000 concurrent threads executed complex SQL joins against PostgreSQL, exhausting
        // HikariCP in 300ms.
        // Entire backend returned HTTP 504 Gateway Timeout.

        Map<String, String> diagnosis =
                Map.of(
                        "Symptom", "Database CPU at 100%, HikariCP pool exhausted, Redis CPU at 2%",
                        "Root Cause",
                                "Hot cache key expired without single-flight mutex or probabilistic pre-refresh",
                        "Immediate Mitigation",
                                "Manually pre-warm hot key in Redis via redis-cli with TTL 24 hours",
                        "Architectural Fix",
                                "Implement distributed lock (SETNX) on cache miss + local L1 Caffeine buffer");

        boolean rootCauseIdentified =
                diagnosis.get("Root Cause").contains("Hot cache key expired"); // true
        boolean immediatePreWarmSavedSystem =
                diagnosis.get("Immediate Mitigation").contains("redis-cli"); // true
    }
}
