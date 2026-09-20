package lab.kafka.questions;

import java.util.List;

/**
 * Q23: Production incident post-mortem: How a malformed JSON payload stalled a financial clearing
 * topic for 6 hours due to missing Dead Letter Topic (DLT) error handling.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q23PoisonPillInfiniteLoopIncident {

    public static void main(String[] args) {
        // Incident chain of events:
        // 1. Upstream partner deployment produces a message with unexpected null fields
        // 2. Consumer's Jackson deserializer throws JsonParseException / NullPointerException
        // 3. Consumer error handler had no DLT configured and retried indefinitely with fixed
        // backoff
        // 4. Consumer offset could not advance past the poison pill offset
        // 5. Partition 3 was 100% frozen; 250,000 downstream settlement records delayed for 6 hours
        List<String> failures =
                List.of(
                        "No Dead Letter Topic (DLT) routing",
                        "Infinite retry loop on non-transient deserialization exception",
                        "Lack of consumer lag alert on individual partitions");

        // Remediation:
        // 1. Spring Kafka ErrorHandlingDeserializer delegating poison pills to CommonErrorHandler
        // 2. DeadLetterPublishingRecoverer to publish malformed records directly to topic.DLT
        // 3. Partition-level consumer lag alerting triggering PagerDuty on persistent lag > 500
        boolean poisonPillSafeguardActive = true; // true
    }
}
