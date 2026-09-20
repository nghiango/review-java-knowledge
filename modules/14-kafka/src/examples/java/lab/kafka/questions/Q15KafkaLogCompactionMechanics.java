package lab.kafka.questions;

/**
 * Q15: How does Kafka log compaction retain only the latest state per key, and what are tombstone
 * records?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q15KafkaLogCompactionMechanics {

    public static void main(String[] args) {
        // cleanup.policy=compact
        // The cleaner thread scans dirty log segments and removes older records that share the same
        // key,
        // retaining only the record with the highest offset per key.
        boolean retainsLatestPerKey = true; // true

        // Tombstone record: A record with a non-null key and a NULL payload value.
        // Signals deletion of the key from stateful consumers/caches.
        // Cleaner retains tombstone until delete.retention.ms expires, allowing downstream
        // consumers
        // to observe the deletion before permanent compaction purging.
        byte[] tombstoneValue = null;
        boolean isTombstone = (tombstoneValue == null); // true

        // Ideal for event-sourced state changelogs (e.g. user profiles, product prices, Kafka
        // Streams KTable)
        String logCompactionUse = "ChangelogStateStore"; // "ChangelogStateStore"
    }
}
