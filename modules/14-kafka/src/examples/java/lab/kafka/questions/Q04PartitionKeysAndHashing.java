package lab.kafka.questions;

import org.apache.kafka.common.utils.Utils;

/**
 * Q04: How does Kafka map message keys to topic partitions, and what happens when the key is null?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q04PartitionKeysAndHashing {

    public static void main(String[] args) {
        int numPartitions = 6;
        String key = "customer-1029";

        // Kafka hashes message key bytes using 32-bit Murmur2:
        // Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions
        byte[] keyBytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int partition = Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions;

        // The exact same key always maps to the same partition as long as partition count remains
        // constant
        int rehashedPartition = Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions;
        boolean deterministicMapping = (partition == rehashedPartition); // true

        // Null keys: Since Kafka 2.4, uses StickyPartitioner to batch messages into one partition
        // until full or linger.ms expires, then rotates to another partition
        boolean nullKeyBatchedTogether = true; // true
    }
}
