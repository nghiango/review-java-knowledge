package lab.distributeddata.questions;

public class Q12DeadLetterQueuesInDistributedDataPipelinesExample {

    record PoisonPillTriage(String messageId, int retryCount, boolean sentToDlq) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // If an outbox event cannot be deserialized or fails after maximum retries,
        // it must be quarantined to a Dead Letter Topic / Queue rather than blocking partition
        // progress.
        PoisonPillTriage triage = new PoisonPillTriage("evt-poison", 3, true);

        boolean isQuarantined = triage.sentToDlq(); // true
        boolean stopsPartitionBlockage = isQuarantined && triage.retryCount() >= 3; // true

        System.out.println("Poison pill diverted to DLQ: " + stopsPartitionBlockage);
    }
}
