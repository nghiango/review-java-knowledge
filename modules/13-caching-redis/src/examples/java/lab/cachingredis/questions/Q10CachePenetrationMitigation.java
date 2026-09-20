package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q10: What is Cache Penetration, and how do Bloom Filters and Sentinel Null Objects mitigate it?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q10CachePenetrationMitigation {

    public static void main(String[] args) {
        // Cache Penetration:
        // Incoming requests query non-existent keys (e.g. invalid IDs). Since the data does not
        // exist
        // in DB or cache, every request bypasses the cache and directly hammers the database.
        Map<String, String> solutions =
                Map.of(
                        "Sentinel Null Caching",
                        "Cache a dummy NULL value with a short TTL (e.g. 30–60s). Fast to implement; uses small memory.",
                        "Bloom Filter",
                        "Bit array and k hash functions checking key existence. If false: guaranteed not in DB; if true: probable.",
                        "Input Validation",
                        "Reject invalid UUIDs, negative numbers, or malformed slugs at the API gateway layer.");

        boolean bloomFilterGuaranteesNonExistence =
                solutions.get("Bloom Filter").contains("guaranteed not in DB"); // true
        boolean sentinelNullUsesShortTtl =
                solutions.get("Sentinel Null Caching").contains("short TTL"); // true
    }
}
