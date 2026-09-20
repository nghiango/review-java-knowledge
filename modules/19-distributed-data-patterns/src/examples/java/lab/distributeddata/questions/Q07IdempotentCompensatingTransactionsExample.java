package lab.distributeddata.questions;

import java.util.HashSet;
import java.util.Set;

public class Q07IdempotentCompensatingTransactionsExample {

    record CompensationLedger(Set<String> processedRefundOrderIds) {
        boolean applyRefund(String orderId, double amount) {
            // Must be strictly idempotent: if refund already issued, skip without altering balance
            return processedRefundOrderIds.add(orderId);
        }
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        CompensationLedger ledger = new CompensationLedger(new HashSet<>());

        boolean firstRefundSuccess = ledger.applyRefund("order-101", 50.0); // true
        boolean redeliveryRefundSkipped = !ledger.applyRefund("order-101", 50.0); // true

        System.out.println(
                "First compensation applied: "
                        + firstRefundSuccess
                        + ", Duplicate compensation skipped: "
                        + redeliveryRefundSkipped);
    }
}
