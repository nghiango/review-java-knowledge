package lab.kafka.questions;

import java.util.Map;

/** Q07: Is "Exactly-Once Processing" achievable across end-to-end distributed systems? */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q07ExactlyOnceSemanticsMyths {

    public static void main(String[] args) {
        // Kafka Transactions (read_process_write loop within Kafka) guarantee exactly-once
        // ONLY between Kafka topics (consume from Topic A, publish to Topic B, commit consumer
        // offset in same tx)
        boolean exactlyOnceWithinKafkaLogs = true; // true

        // End-to-end to external databases or third-party HTTP APIs CANNOT achieve magical 2PC
        // exactly-once
        // Across non-Kafka systems without idempotent receiver or 2-phase commit protocol
        boolean externalSystemsRequireIdempotency = true; // true

        Map<String, String> architecture =
                Map.of(
                        "Kafka-to-Kafka",
                                "Kafka Transactions (Transactional Producer + read_committed Consumer)",
                        "Kafka-to-Database", "Idempotent Consumer + Atomic DB Deduplication Store");

        boolean databaseNeedsDeduplication =
                architecture.get("Kafka-to-Database").contains("Idempotent Consumer"); // true
    }
}
