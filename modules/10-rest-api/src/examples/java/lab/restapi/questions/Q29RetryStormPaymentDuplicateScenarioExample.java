package lab.restapi.questions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public final class Q29RetryStormPaymentDuplicateScenarioExample {
    private Q29RetryStormPaymentDuplicateScenarioExample() {}

    public static class PaymentGatewaySimulator {
        private final ConcurrentHashMap<String, String> processedIdempotencyKeys = new ConcurrentHashMap<>();
        private final AtomicInteger actualBankCharges = new AtomicInteger(0);

        // Without Idempotency-Key: every retry charges the customer credit card!
        public String chargeWithoutIdempotency(String accountId, int amountCents) {
            actualBankCharges.incrementAndGet(); // Charges credit card!
            return "txn-" + actualBankCharges.get();
        }

        // With Idempotency-Key: subsequent retries return the original transaction receipt
        public String chargeWithIdempotency(String idempotencyKey, String accountId, int amountCents) {
            return processedIdempotencyKeys.computeIfAbsent(idempotencyKey, key -> {
                actualBankCharges.incrementAndGet();
                return "txn-idempotent-" + actualBankCharges.get();
            });
        }

        public int getChargeCount() { return actualBankCharges.get(); }
    }

    public static void main(String[] args) {
        PaymentGatewaySimulator gateway = new PaymentGatewaySimulator();

        // Client timeout retry storm (3 retries with SAME idempotency key):
        String receipt1 = gateway.chargeWithIdempotency("order-tx-1234", "acc-1", 5000);
        String receipt2 = gateway.chargeWithIdempotency("order-tx-1234", "acc-1", 5000);
        String receipt3 = gateway.chargeWithIdempotency("order-tx-1234", "acc-1", 5000);

        // Only 1 charge executed!
        int charges = gateway.getChargeCount(); // 1
        boolean exactlyOneCharge = (charges == 1); // true
    }
}
