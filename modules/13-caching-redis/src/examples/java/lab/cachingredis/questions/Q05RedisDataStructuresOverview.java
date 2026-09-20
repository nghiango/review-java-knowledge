package lab.cachingredis.questions;

import java.util.List;
import java.util.Map;

/** Q05: What core data structures does Redis provide, and what is the optimal use case for each? */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q05RedisDataStructuresOverview {

    public static void main(String[] args) {
        Map<String, String> dataStructures =
                Map.of(
                        "STRING",
                                "Caches JSON objects, session tokens, binary values, atomic counters (INCR)",
                        "HASH",
                                "Objects with multiple fields (HGET, HSET) saving memory via ziplists",
                        "LIST", "Ordered message queues, activity timelines (LPUSH, RPOP, BLPOP)",
                        "SET",
                                "Unique collections, tags, mutual friends, set intersection (SADD, SINTER)",
                        "SORTED_SET",
                                "Leaderboards, rate limiter sliding windows with timestamp score (ZADD, ZRANGE)");

        boolean sortedSetIdealForLeaderboards =
                dataStructures.get("SORTED_SET").contains("Leaderboards"); // true
        boolean hashSavesMemoryForObjects = dataStructures.get("HASH").contains("ziplists"); // true

        // Advanced data structures:
        List<String> advancedStructures =
                List.of(
                        "BITMAPS", // Compact user daily login tracking (1 bit per user)
                        "HYPERLOGLOG", // Unique visitor cardinality estimation with bounded 12KB
                        // memory
                        "GEOSPATIAL", // Coordinates and radius queries (GEOADD, GEORADIUS)
                        "STREAMS" // Append-only log with consumer groups (XADD, XREADGROUP)
                        );

        boolean hyperLogLogApproximatesCardinality =
                advancedStructures.contains("HYPERLOGLOG"); // true
    }
}
