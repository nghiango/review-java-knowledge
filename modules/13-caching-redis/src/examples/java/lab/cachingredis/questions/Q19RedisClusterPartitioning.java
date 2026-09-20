package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q19: How does Redis Cluster partition data using Hash Slots, and how do Hash Tags ({...}) enable
 * multi-key operations?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q19RedisClusterPartitioning {

    public static void main(String[] args) {
        // Redis Cluster uses 16,384 fixed hash slots:
        // HASH_SLOT = CRC16(key) mod 16384
        int totalHashSlots = 16384;
        boolean slotsTotal16384 = (totalHashSlots == 16384); // true

        // Multi-key operations (MGET, MSET, transactions, Lua scripts) are ONLY permitted
        // if all keys hash to the EXACT SAME slot on the same Redis cluster node (CROSSSLOT error).
        // Solution: Hash Tags:
        // Only the text inside {...} is passed to the CRC16 hash function.
        String key1 = "{user:101}:profile";
        String key2 = "{user:101}:orders";

        // Both keys are guaranteed to reside on the same hash slot and Redis shard node!
        String tag = "{user:101}";
        boolean sameSlotGuaranteed = key1.startsWith(tag) && key2.startsWith(tag); // true

        Map<String, String> clusterConcepts =
                Map.of(
                        "Hash Slots", "16384 slots distributed across master nodes",
                        "CrossSlot Error", "Thrown when multi-key command spans multiple slots",
                        "Hash Tags", "Enforces co-location of related keys onto a single slot");

        boolean hashTagsSolveCrossSlot =
                clusterConcepts.get("Hash Tags").contains("co-location"); // true
    }
}
