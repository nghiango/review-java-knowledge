package lab.resilience.questions;

import java.util.UUID;

public class Q11IdempotencyInRetryingPostCallsExample {

    record HttpRequest(String method, String uri, String idempotencyKey, String payload) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Mutating POST requests require an immutable client-generated Idempotency-Key
        String clientToken = UUID.randomUUID().toString();

        HttpRequest attempt1 =
                new HttpRequest("POST", "/api/v1/charges", clientToken, "{\"amount\":100}");
        HttpRequest attempt2 =
                new HttpRequest("POST", "/api/v1/charges", clientToken, "{\"amount\":100}");

        boolean keysMatch = attempt1.idempotencyKey().equals(attempt2.idempotencyKey()); // true
        boolean preventsDoubleCharge = keysMatch && "POST".equals(attempt1.method()); // true

        System.out.println("Same idempotency key across retries: " + preventsDoubleCharge);
    }
}
