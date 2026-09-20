package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q07: How do Redis memory eviction policies (maxmemory-policy) operate when memory limits are
 * reached?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q07CacheEvictionPolicies {

    public static void main(String[] args) {
        Map<String, String> policies =
                Map.of(
                        "noeviction",
                                "Returns OOM error on writes when maxmemory is exceeded (Redis default)",
                        "allkeys-lru",
                                "Evicts least-recently-used keys among ALL keys; recommended for pure cache instances",
                        "volatile-lru",
                                "Evicts LRU keys ONLY among keys with an expiration TTL set",
                        "allkeys-lfu",
                                "Evicts least-frequently-used keys among ALL keys using logarithmic access counter",
                        "volatile-ttl",
                                "Evicts keys with the shortest remaining time-to-live first");

        // If Redis holds both long-lived session tokens without TTL and short-lived entity caches:
        // volatile-lru protects session tokens, but can exhaust memory if too many session tokens
        // accumulate.
        boolean noEvictionRejectsWrites = policies.get("noeviction").contains("OOM error"); // true

        // In pure caching layers, allkeys-lru or allkeys-lfu is preferred to guarantee zero OOM
        // rejections.
        boolean allKeysLruRecommendedForPureCache =
                policies.get("allkeys-lru").contains("recommended"); // true
    }
}
