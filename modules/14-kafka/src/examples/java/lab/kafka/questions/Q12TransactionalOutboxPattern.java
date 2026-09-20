package lab.kafka.questions;

import java.util.List;

/**
 * Q12: How does the Transactional Outbox pattern solve the dual-write consistency hazard between
 * SQL DB and Kafka?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q12TransactionalOutboxPattern {

    public static void main(String[] args) {
        // Dual-write problem:
        // Writing to DB and publishing to Kafka cannot participate in a single atomic commit
        // without 2PC.
        // If DB commits but Kafka publish fails: lost event.
        // If Kafka publish succeeds but DB rollback occurs: ghost event.
        boolean dualWriteHazardous = true; // true

        // Outbox pattern:
        // 1. Business state update and event insertion into 'outbox_table' happen in SAME ACID DB
        // transaction
        // 2. Outbox poller or CDC engine (e.g. Debezium reading Postgres WAL) publishes outbox
        // events to Kafka
        // 3. Mark outbox entry as published or delete it
        List<String> outboxSteps =
                List.of("Atomic DB write", "CDC/WAL tailing", "Kafka publishing");
        boolean guaranteesAtLeastOncePublishing = outboxSteps.size() == 3; // true
    }
}
