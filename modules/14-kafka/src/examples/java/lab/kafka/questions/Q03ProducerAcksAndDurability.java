package lab.kafka.questions;

/**
 * Q03: What are the differences between acks=0, acks=1, and acks=all (-1), and what does
 * enable.idempotence provide?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q03ProducerAcksAndDurability {

    public static void main(String[] args) {
        // acks=0: Fire and forget; zero network round-trip wait, highest risk of data loss
        String acks0Guarantee = "None"; // "None"

        // acks=1: Leader writes to local log and responds; data loss if leader crashes before
        // replication
        String acks1Guarantee = "LeaderOnly"; // "LeaderOnly"

        // acks=all (-1): Leader waits for full min.insync.replicas commit before responding
        String acksAllGuarantee = "StrongestDurability"; // "StrongestDurability"

        // enable.idempotence=true: Assigns ProducerId (PID) and sequence numbers to records.
        // Prevents duplicate records on broker if producer retries due to network ack timeout.
        boolean duplicateRetryEliminated = true; // true
        boolean inOrderPerPartitionGuaranteed = true; // true
    }
}
