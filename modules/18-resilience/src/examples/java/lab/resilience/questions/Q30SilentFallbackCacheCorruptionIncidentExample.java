package lab.resilience.questions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Q30: Production Incident: Silent fallback masked complete downstream database failure while poisoning read caches.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q30SilentFallbackCacheCorruptionIncidentExample {

    public static void main(String[] args) {
        // Incident Scenario:
        // A catalog service had a CircuitBreaker with fallbackMethod = "emptyListFallback".
        // public List<Product> emptyListFallback(Throwable t) { return Collections.emptyList(); }
        //
        // Failure Sequence:
        // 1. The product database crashed under a network partition.
        // 2. The circuit breaker tripped to OPEN and gracefully returned emptyList() without errors.
        // 3. Upstream caching layer (@Cacheable or Redis template) intercepted the returned emptyList()
        //    and cached it as the authoritative product catalog for 24 hours!
        // 4. Entire e-commerce storefront displayed "0 products found" for all categories across all users.
        // 5. Zero alerts fired because the service returned HTTP 200 with empty JSON instead of HTTP 5xx!

        List<String> badFallbackResult = Collections.emptyList();
        boolean masksOutageAsSuccess = badFallbackResult.isEmpty(); // true

        // Remediation:
        // 1. Do not cache fallback results! Use condition/unless in Spring Cache:
        //    @Cacheable(unless = "#result == null || #result.isEmpty()")
        // 2. Increment a dedicated Micrometer metric counter in fallback methods to alert on fallback volume.
        // 3. For critical read paths, prefer failing fast with HTTP 503 rather than returning deceptive empty results.

        Map<String, String> cachePolicy =
                Map.of(
                        "Naive Fallback", "Returns empty list; poisons cache with 0 items; 0 alerts fired",
                        "Hardened Fallback", "unless='#result.isEmpty()'; increments fallback counter; triggers PagerDuty");

        boolean preventsCachePoisoning =
                cachePolicy.get("Hardened Fallback").contains("unless="); // true
    }
}
