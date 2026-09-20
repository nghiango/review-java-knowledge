package lab.restapi.questions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Q20PaymentDuplicateRetryIncident {

    public static void main(String[] args) {
        // Incident: Gateway timeout caused HTTP client to retry charge 3 times.
        // Without idempotency, account is debited 3 times ($300 total).
        AtomicInteger nonIdempotentCharges = new AtomicInteger(0);
        for (int i = 0; i < 3; i++) {
            nonIdempotentCharges.incrementAndGet();
        }
        int totalDebitsWithoutIdempotency = nonIdempotentCharges.get(); // 3

        // Solution: Idempotent charging using Idempotency-Key
        ConcurrentHashMap<String, String> processedKeys = new ConcurrentHashMap<>();
        AtomicInteger idempotentCharges = new AtomicInteger(0);
        String idempotencyKey = "tx-retry-token-5544";

        for (int i = 0; i < 3; i++) {
            processedKeys.computeIfAbsent(
                    idempotencyKey,
                    k -> {
                        idempotentCharges.incrementAndGet();
                        return "CHARGED_100_USD";
                    });
        }
        int totalDebitsWithIdempotency = idempotentCharges.get(); // 1

        System.out.println(
                "Non-idempotent total debits: "
                        + totalDebitsWithoutIdempotency); // Non-idempotent total debits: 3
        System.out.println(
                "Idempotent total debits: "
                        + totalDebitsWithIdempotency); // Idempotent total debits: 1
    }
}
