package lab.kafka.questions;

import java.util.List;

/**
 * Q13: How do Kafka transactions (transactional.id, Transaction Coordinator, 2PC marker) work under
 * the hood?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q13KafkaTransactionsTwoPhaseCommit {

    public static void main(String[] args) {
        // Transactional producer requires a unique transactional.id across restarts
        String transactionalId = "order-stream-processor-1";

        // Protocol sequence:
        // 1. initTransactions() registers PID with Transaction Coordinator broker
        // 2. beginTransaction()
        // 3. send() records to topic partitions
        // 4. sendOffsetsToTransaction() commits consumer group offsets within the same atomic
        // boundary
        // 5. commitTransaction() / abortTransaction() writes COMMIT/ABORT control markers to topic
        // logs
        List<String> protocol =
                List.of(
                        "initTransactions",
                        "beginTransaction",
                        "sendRecords",
                        "sendOffsetsToTransaction",
                        "commitTransaction");

        // Consumers configured with isolation.level=read_committed only see records whose
        // transaction
        // has a COMMIT marker, buffering uncommitted records until the transaction completes or
        // aborts
        String consumerIsolationLevel = "read_committed";
        boolean skipsAbortedTransactions = consumerIsolationLevel.equals("read_committed"); // true
    }
}
