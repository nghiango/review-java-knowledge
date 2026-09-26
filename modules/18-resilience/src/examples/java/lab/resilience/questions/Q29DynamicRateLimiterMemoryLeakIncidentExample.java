package lab.resilience.questions;

import java.util.Map;

/**
 * Q29: Production Incident: JVM OOM and CPU lockup caused by dynamic unbounded RateLimiter registry keys.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q29DynamicRateLimiterMemoryLeakIncidentExample {

    public static void main(String[] args) {
        // Incident Scenario:
        // A developer added per-client rate limiting to an API gateway:
        // rateLimiterRegistry.rateLimiter(clientId, config)
        // In production, unauthenticated bot crawlers and attackers sent random UUIDs as clientId.

        // Failure Mechanism:
        // 1. Resilience4j RateLimiterRegistry stores instances in a ConcurrentHashMap keyed by name.
        // 2. Each RateLimiter instance allocates internal AtomicReference states, nano-timers, and metrics hooks.
        // 3. 2 million distinct clientIds over 4 hours allocated 2 million active RateLimiter objects.
        // 4. RateLimiterRegistry memory footprint grew to 8GB, exhausting JVM heap and triggering
        //    non-stop Full GC pauses followed by OutOfMemoryError.

        boolean dynamicUnboundedKeysCauseOom = true; // true

        // Remediation:
        // 1. Never create dynamic Resilience4j RateLimiter instances per unbounded entity ID (user, IP, UUID).
        // 2. Use Resilience4j strictly for static tier rate limiting (e.g., "tier-anonymous", "tier-gold").
        // 3. For high-cardinality distributed rate limiting (per-user/per-IP), delegate to Redis Token Bucket
        //    with an automatic key TTL (EXPIRE key 60).

        Map<String, String> architecture =
                Map.of(
                        "Resilience4j Registry", "Designed for static service-level limits; unbounded keys leak heap",
                        "Redis Sliding Window", "Designed for millions of keys; automatic memory reclamation via TTL");

        boolean redisBetterForHighCardinality =
                architecture.get("Redis Sliding Window").contains("millions of keys"); // true
    }
}
