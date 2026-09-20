package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q01: What are the fundamental caching patterns (Cache-Aside, Read-Through, Write-Through,
 * Write-Behind), and when should each be used?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q01CachePatternsComparison {

    public static void main(String[] args) {
        // Cache-Aside (Lazy Loading):
        // Application code directly queries the cache. On miss, application reads DB and writes to
        // cache.
        String cachePattern1 = "Cache-Aside";
        boolean appCoordinatesBoth = "Cache-Aside".equals(cachePattern1); // true

        // Read-Through:
        // Application treats cache as the primary store. Cache library itself fetches from DB on
        // miss.
        boolean transparentRead = true; // true

        // Write-Through:
        // Application writes to cache, and cache synchronously writes to underlying DB before
        // returning.
        boolean synchronousDbWrite = true; // true

        // Write-Behind (Write-Back):
        // Application writes to cache, cache acknowledges immediately and asynchronously flushes to
        // DB in batches.
        boolean asyncBatchWrite = true; // true

        Map<String, String> tradeOffs =
                Map.of(
                        "Cache-Aside",
                                "Resilient to cache outages; possible stale data without eviction",
                        "Write-Through",
                                "High consistency; write latency penalty on every mutation",
                        "Write-Behind",
                                "Extremely low write latency; risk of data loss if cache crashes before flush");

        boolean cacheAsideHasFallback = tradeOffs.get("Cache-Aside").contains("Resilient"); // true
    }
}
