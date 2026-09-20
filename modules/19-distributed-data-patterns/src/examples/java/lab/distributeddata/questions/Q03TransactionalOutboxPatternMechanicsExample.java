package lab.distributeddata.questions;

public class Q03TransactionalOutboxPatternMechanicsExample {

    record OutboxTransaction(String orderId, String eventPayload, boolean inSameLocalTx) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // In the Transactional Outbox pattern, the business entity and outbox event
        // are committed in the EXACT SAME local database transaction.
        OutboxTransaction tx = new OutboxTransaction("ORD-101", "{\"orderId\":\"ORD-101\"}", true);

        boolean isAtomicInLocalDb = tx.inSameLocalTx(); // true
        System.out.println(
                "Outbox table guarantees atomicity via local RDBMS transaction: "
                        + isAtomicInLocalDb);
    }
}
