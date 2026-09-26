package lab.architecture.questions;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Demonstrates a production scenario resolving distributed transaction inconsistency where dual writes
 * (saving to database then publishing to Kafka) caused ghost records upon broker network partition.
 *
 * <p>Solution: The Transactional Outbox pattern writes the domain state mutation and the outbox event
 * in a single local database transaction, guaranteeing atomic consistency and zero message loss.</p>
 */
public class Q29DualWriteInconsistencyIncidentExample {

    public static class OutboxDatabaseSimulator {
        private boolean entitySaved = false;
        private boolean outboxRecordSaved = false;

        public void executeAtomicOutboxTransaction() {
            // Both entity mutation and outbox event are committed in the same DB transaction
            this.entitySaved = true;
            this.outboxRecordSaved = true;
        }

        public boolean isAtomicallyCommitted() {
            return entitySaved && outboxRecordSaved;
        }
    }

    public static void main(String[] args) {
        OutboxDatabaseSimulator db = new OutboxDatabaseSimulator();
        db.executeAtomicOutboxTransaction();

        boolean atomicConsistencyGuaranteed = db.isAtomicallyCommitted(); // true
        System.out.println("Outbox and entity saved atomically: " + atomicConsistencyGuaranteed);
    }
}
