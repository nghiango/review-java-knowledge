package lab.kafka.questions;

import org.springframework.kafka.listener.ContainerProperties;

/**
 * Q09: What key settings on ConcurrentKafkaListenerContainerFactory tune concurrency, error
 * handling, and ack modes?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q09SpringKafkaListenerConfiguration {

    public static void main(String[] args) {
        // Concurrency sets the number of concurrent KafkaMessageListenerContainer threads spawned
        int concurrency = 4; // Up to number of partitions

        // AckMode options:
        // RECORD: commit after each record processed
        // BATCH: commit after all records in poll() processed
        // MANUAL_IMMEDIATE: commit immediately when Acknowledgment.acknowledge() is called
        ContainerProperties.AckMode ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE;
        boolean manualImmediateActive =
                ackMode.equals(ContainerProperties.AckMode.MANUAL_IMMEDIATE); // true

        // Batch listener vs Record listener:
        // Set factory.setBatchListener(true) to receive List<ConsumerRecord> or List<T>
        boolean batchListenerSupported = true; // true
    }
}
