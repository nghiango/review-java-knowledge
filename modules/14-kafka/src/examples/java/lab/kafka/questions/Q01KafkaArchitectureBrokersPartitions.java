package lab.kafka.questions;

/**
 * Q01: How does Kafka achieve high write and read throughput through its commit log architecture
 * and sequential disk I/O?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q01KafkaArchitectureBrokersPartitions {

    public static void main(String[] args) {
        // Partitions are immutable append-only commit logs on disk
        boolean isAppendOnly = true; // true

        // Sequential disk writes avoid random disk seek overhead, matching or exceeding memory bus
        // speeds
        boolean sequentialDiskFasterThanRandomMemory = true; // true

        // Read path leverages the OS Page Cache directly without JVM garbage collection overhead
        boolean reliesOnOsPageCache = true; // true

        // A topic with 12 partitions allows at most 12 active consumers in the same consumer group
        int partitions = 12;
        int maxActiveConsumersPerGroup = partitions; // 12
    }
}
