package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q29: Production Incident: Redis cluster blocked and timed out due to KEYS * pattern evaluation.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q29KeysWildcardPatternScanOomIncident {

    public static void main(String[] args) {
        // Incident Scenario:
        // A developer added an admin endpoint to clear all user session caches matching "session:user:*".
        // The implementation called redisTemplate.keys("session:user:*") in production on a dataset
        // containing 25 million keys.

        // Failure Mechanism:
        // 1. Redis is single-threaded for command execution.
        // 2. KEYS is an O(N) blocking operation that scans every single key in the dictionary.
        // 3. For 25M keys, KEYS took 6.5 seconds to execute.
        // 4. During those 6.5 seconds, ALL incoming requests (GET, SET, PING) were blocked.
        // 5. HikariCP and Lettuce client timeouts triggered cascading HTTP 504 errors across 40 microservices.
        // 6. Redis Sentinel marked the master as ODOWN and triggered an unwanted failover!

        boolean keysIsBlockingSingleThread = true; // true

        // Remediation:
        // 1. Disable KEYS command in redis.conf: rename-command KEYS ""
        // 2. Replace with cursor-based SCAN:
        //    SCAN cursor MATCH pattern COUNT 1000
        //    Returns a cursor and a slice of keys incrementally, interleaving between other client requests.

        Map<String, String> commandComparison =
                Map.of(
                        "KEYS", "O(N) blocking; freezes Redis event loop; banned in production",
                        "SCAN", "Cursor-based pagination; non-blocking; safe for live production");

        boolean scanIsSafeInProduction =
                commandComparison.get("SCAN").contains("non-blocking"); // true
    }
}
