package lab.restapi.questions;

import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public final class Q25IetfIdempotencyKeySpecExample {
    private Q25IetfIdempotencyKeySpecExample() {}

    public record CachedResponse(int httpStatus, String body, String requestFingerprint) {}

    // In-memory simulation of IETF Idempotency-Key storage:
    // When a POST request arrives with header "Idempotency-Key":
    // 1. If key not seen: process request, store (status, body, requestHash), and return response.
    // 2. If key seen with matching requestHash: return cached response immediately without
    // re-executing business logic.
    // 3. If key seen with DIFFERENT requestHash: reject with HTTP 422 Unprocessable Entity
    // (Idempotency Key Conflict)!
    public static class IdempotencyManager {
        private final ConcurrentHashMap<String, CachedResponse> store = new ConcurrentHashMap<>();

        public CachedResponse handleRequest(String idempotencyKey, String payloadHash) {
            return store.computeIfAbsent(
                    idempotencyKey,
                    key -> {
                        // Execute business logic:
                        return new CachedResponse(
                                201,
                                "{\"orderId\":\"ord-456\",\"status\":\"CREATED\"}",
                                payloadHash);
                    });
        }
    }

    public static void main(String[] args) {
        IdempotencyManager manager = new IdempotencyManager();
        // First execution creates the order:
        CachedResponse first = manager.handleRequest("key-uuid-1", "hash-abc");
        int status1 = first.httpStatus(); // 201

        // Retry with same key returns identical cached response without recharging:
        CachedResponse retry = manager.handleRequest("key-uuid-1", "hash-abc");
        int status2 = retry.httpStatus(); // 201 (replayed from idempotency store!)
    }
}
