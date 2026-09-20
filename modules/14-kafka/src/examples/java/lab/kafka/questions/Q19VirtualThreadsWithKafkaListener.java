package lab.kafka.questions;

import java.util.concurrent.Executors;

/**
 * Q19: How do Java 21 Virtual Threads integrate with Spring Kafka listeners, and what are the
 * concurrency benefits?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q19VirtualThreadsWithKafkaListener {

    public static void main(String[] args) {
        // Concurrency scaling in Spring Kafka is fundamentally bound to partition count per
        // consumer group:
        // A single partition can only be consumed by at most ONE active consumer thread in a group.
        int topicPartitions = 10;
        int maxUsefulConsumerThreads = topicPartitions; // 10

        // However, within each listener container or for asynchronous dispatching (e.g. downstream
        // DB / HTTP calls),
        // virtual thread executors allow thousands of concurrent non-blocking I/O tasks:
        var virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        boolean isVirtualExecutorAvailable = (virtualExecutor != null); // true

        // Setting spring.threads.virtual.enabled=true in Spring Boot 3.2+ configures virtual
        // threads
        // for Spring Kafka listeners and task executors automatically
        boolean zeroKernelThreadMemoryOverhead = true; // true
        virtualExecutor.close();
    }
}
