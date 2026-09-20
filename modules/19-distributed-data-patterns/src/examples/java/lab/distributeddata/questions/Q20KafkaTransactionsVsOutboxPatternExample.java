package lab.distributeddata.questions;

public class Q20KafkaTransactionsVsOutboxPatternExample {

    record ArchitectureComparison(
            String pattern, boolean spansRelationalDbAndBroker, boolean requiresDedicatedKafkaTx) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Kafka Transactions (read-process-write across Kafka topics) CANNOT atomically span an
        // external PostgreSQL database!
        // The Transactional Outbox pattern is required when coordinating between an RDBMS and
        // Kafka.
        ArchitectureComparison kafkaTx =
                new ArchitectureComparison("KafkaTransactions", false, true);
        ArchitectureComparison outbox =
                new ArchitectureComparison("TransactionalOutbox", true, false);

        boolean kafkaTxLimitedToKafka = !kafkaTx.spansRelationalDbAndBroker(); // true
        boolean outboxSpansDbAndBroker = outbox.spansRelationalDbAndBroker(); // true

        System.out.println(
                "Kafka transactions limited to Kafka cluster: "
                        + kafkaTxLimitedToKafka
                        + ", Outbox spans RDBMS and broker: "
                        + outboxSpansDbAndBroker);
    }
}
