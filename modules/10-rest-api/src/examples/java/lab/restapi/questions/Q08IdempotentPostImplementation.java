package lab.restapi.questions;

import java.util.concurrent.ConcurrentHashMap;

public class Q08IdempotentPostImplementation {

    private static final ConcurrentHashMap<String, String> IDEMPOTENCY_STORE =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {
        String idempotencyKey = "req-uuid-9876";

        // First execution: key absent, processes transaction
        String firstResult = processWithIdempotency(idempotencyKey, "TX-1001-OK"); // "TX-1001-OK"
        boolean firstProcessed = "TX-1001-OK".equals(firstResult); // true

        // Second execution with same key: returns cached result without reprocessing
        String secondResult =
                processWithIdempotency(idempotencyKey, "TX-DUPLICATE"); // "TX-1001-OK"
        boolean duplicateReturnedOriginal = "TX-1001-OK".equals(secondResult); // true

        System.out.println(
                "First call processed: "
                        + firstProcessed
                        + " with result: "
                        + firstResult); // First call processed: true with result: TX-1001-OK
        System.out.println(
                "Second call replay: "
                        + duplicateReturnedOriginal
                        + " with result: "
                        + secondResult); // Second call replay: true with result: TX-1001-OK
    }

    public static String processWithIdempotency(String key, String outcomeToStore) {
        return IDEMPOTENCY_STORE.computeIfAbsent(key, k -> outcomeToStore);
    }
}
