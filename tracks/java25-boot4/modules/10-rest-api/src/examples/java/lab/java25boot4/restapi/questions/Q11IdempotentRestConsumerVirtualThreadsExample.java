package lab.java25boot4.restapi.questions;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Q11: How should distributed idempotency keys be managed in high-concurrency virtual-thread REST
 * APIs to avoid race conditions and duplicate operations?
 */
public class Q11IdempotentRestConsumerVirtualThreadsExample {

    public record IdempotencyRecord(String key, String status, String responsePayload) {}

    public static class InMemoryIdempotencyStore {
        private final ConcurrentHashMap<String, IdempotencyRecord> store =
                new ConcurrentHashMap<>();

        public boolean tryAcquireLock(String key) {
            return store.putIfAbsent(key, new IdempotencyRecord(key, "PROCESSING", "")) == null;
        }

        public void complete(String key, String payload) {
            store.put(key, new IdempotencyRecord(key, "COMPLETED", payload));
        }

        public IdempotencyRecord get(String key) {
            return store.get(key);
        }
    }

    public static void main(String[] args) {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        String key = "IDEMP-KEY-9988";

        boolean firstAttempt = store.tryAcquireLock(key);
        boolean secondAttempt = store.tryAcquireLock(key);

        store.complete(key, "{\"status\":\"CONFIRMED\"}");

        System.out.println("First lock acquired: " + firstAttempt); // true
        System.out.println("Second duplicate rejected: " + !secondAttempt); // true
        System.out.println("Status: " + store.get(key).status()); // "COMPLETED"
    }
}
