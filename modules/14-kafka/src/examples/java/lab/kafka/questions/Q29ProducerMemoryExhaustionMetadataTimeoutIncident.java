package lab.kafka.questions;

import java.util.Map;

/**
 * Q29: Production Incident: JVM OOM crash caused by Kafka producer buffer pool exhaustion during network partition.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q29ProducerMemoryExhaustionMetadataTimeoutIncident {

    public static void main(String[] args) {
        // Incident Scenario:
        // A sudden network partition isolated the Kafka broker hosting the leader of partition 'payments-0'.
        // The Java producer was configured with defaults:
        // - buffer.memory = 33554432 (32 MB)
        // - max.block.ms = 60000 (60 seconds)
        //
        // Failure Sequence:
        // 1. In-flight send() requests could not flush because broker leader was unreachable.
        // 2. The 32 MB RecordAccumulator buffer filled up within 400ms.
        // 3. Subsequent producer.send() invocations blocked the incoming HTTP request threads (Tomcat worker pool).
        // 4. All 200 Tomcat worker threads blocked waiting for buffer space in RecordAccumulator.
        // 5. Incoming HTTP requests queued in memory, causing heap exhaustion and triggering a fatal JVM OutOfMemoryError.

        boolean bufferExhaustionBlocksCallingThread = true; // true

        // Remediation:
        // 1. Lower max.block.ms from 60s to 1500ms to fail fast and reject requests with 503 instead of blocking threads.
        // 2. Apply circuit breaker (Resilience4j) around producer.send() to open circuit on consecutive buffer timeouts.
        // 3. Size buffer.memory according to peak burst rate and producer memory limits.

        Map<String, String> configurationRemediation =
                Map.of(
                        "Default max.block.ms", "60,000ms (blocks worker threads; exhausts container memory)",
                        "Hardened max.block.ms", "1,500ms (fails fast; returns 503 Service Unavailable)");

        boolean failsFastUnderBrokerOutage =
                configurationRemediation.get("Hardened max.block.ms").contains("fails fast"); // true
    }
}
