package lab.springtransactions.questions;

import java.util.HashMap;
import java.util.Map;

public class Q20TransactionalOutboxPatternExample {

    record OutboxMessage(String id, String aggregateType, String payload, String status) {}

    public static void main(String[] args) {
        // Transactional Outbox Pattern: Writes business entity AND outbox message within the SAME
        // local DB transaction
        Map<String, String> ordersTable = new HashMap<>();
        Map<String, OutboxMessage> outboxTable = new HashMap<>();

        String orderId = "ord-9921";
        ordersTable.put(orderId, "CONFIRMED");
        outboxTable.put(
                "msg-1",
                new OutboxMessage(
                        "msg-1", "ORDER", "{\"orderId\":\"" + orderId + "\"}", "PENDING"));

        boolean orderSaved = ordersTable.containsKey(orderId); // true
        boolean outboxSaved = outboxTable.containsKey("msg-1"); // true
        boolean atomicSuccess = orderSaved && outboxSaved; // true

        System.out.println(
                "Order and Outbox written in same transaction: "
                        + atomicSuccess
                        + ", outbox status: "
                        + outboxTable.get("msg-1").status());
    }
}
