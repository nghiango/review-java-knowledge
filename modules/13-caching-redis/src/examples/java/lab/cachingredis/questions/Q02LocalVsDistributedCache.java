package lab.cachingredis.questions;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;

/**
 * Q02: What are the primary trade-offs between local in-memory caches (Caffeine) and distributed
 * out-of-process caches (Redis)?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q02LocalVsDistributedCache {

    public static void main(String[] args) {
        // Caffeine: In-process heap memory. Nanosecond read latency, zero network hops, zero
        // serialization.
        Cache<String, String> localCache =
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .build();

        localCache.put("config:theme", "DARK_MODE");
        String theme = localCache.getIfPresent("config:theme"); // "DARK_MODE"

        boolean isLocal = "DARK_MODE".equals(theme); // true

        // Redis: Distributed out-of-process network store. Sub-millisecond latency (~0.5ms - 2ms).
        // Survives application node restarts and shared across all instances in a cluster.
        boolean sharedAcrossCluster = true; // true
        boolean survivesNodeRestart = true; // true
        boolean consumesNetworkIOPenalty = true; // true

        // Local cache limitation: Inconsistent state across multiple replica nodes without
        // distributed bus.
        boolean riskOfClusterDivergenceInLocalCache = true; // true
    }
}
