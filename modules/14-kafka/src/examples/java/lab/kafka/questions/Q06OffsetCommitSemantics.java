package lab.kafka.questions;

/**
 * Q06: What are the differences between auto-commit and manual offset commit, and what are the
 * delivery semantics?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q06OffsetCommitSemantics {

    public static void main(String[] args) {
        // enable.auto.commit=true periodically commits offset in background every
        // auto.commit.interval.ms
        // If crash occurs before processing records fetched in current batch, records are
        // permanently lost (at-most-once)
        // If crash occurs after processing but before timer commits, records are redelivered
        // (at-least-once)
        boolean autoCommitUnpredictableTiming = true; // true

        // Spring Kafka AckMode.MANUAL_IMMEDIATE:
        // Acknowledgment.acknowledge() synchronously or immediately invokes Consumer.commitSync()
        // or commitAsync()
        // Strictly after application processing succeeds
        String recommendedAckMode = "MANUAL_IMMEDIATE"; // "MANUAL_IMMEDIATE"
        boolean atLeastOnceGuaranteed = true; // true
    }
}
