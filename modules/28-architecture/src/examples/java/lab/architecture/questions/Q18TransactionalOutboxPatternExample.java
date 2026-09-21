package lab.architecture.questions;

import java.util.ArrayList;
import java.util.List;

/**
 * Q18: Distributed Data Management: Dual-Write Hazard and Transactional Outbox. Demonstrates
 * persisting business entity and outbox message in a single local transaction.
 */
public class Q18TransactionalOutboxPatternExample {

    public record OutboxRecord(String eventType, String payload, boolean published) {}

    public static class TransactionSimulator {
        private final List<String> databaseTables = new ArrayList<>();
        private final List<OutboxRecord> outboxTable = new ArrayList<>();

        public void executeInSingleTransaction(String orderData, String eventPayload) {
            // Step 1: Write business entity
            databaseTables.add(orderData);
            // Step 2: Write outbox event in same transaction
            outboxTable.add(new OutboxRecord("OrderCreated", eventPayload, false));
        }

        public int getOutboxSize() {
            return outboxTable.size();
        }
    }

    public static void main(String[] args) {
        TransactionSimulator tx = new TransactionSimulator();
        tx.executeInSingleTransaction("Order: id=101", "{\"orderId\": 101}");

        int outboxCount = tx.getOutboxSize(); // 1 (atomic with DB state)
        boolean hasOutboxEvent = (outboxCount == 1); // true

        System.out.println("Q18 outboxCount: " + outboxCount + ", atomic: " + hasOutboxEvent);
    }
}
