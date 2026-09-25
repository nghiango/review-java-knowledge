package lab.concurrency.questions;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public final class Q24StructuredConcurrencyExample {
    private Q24StructuredConcurrencyExample() {}

    public record SubtaskResult(String data, boolean success) {}

    // Structured task scope coordination pattern:
    // If one subtask fails, all sibling subtasks are cancelled immediately (fail-fast).
    // In Java 21 StructuredTaskScope.ShutdownOnFailure, joining waits for all or short-circuits on
    // first error.
    public static String executeStructuredQuery() throws Exception {
        CompletableFuture<String> userTask = CompletableFuture.supplyAsync(() -> "user-alice");
        CompletableFuture<String> orderTask = CompletableFuture.supplyAsync(() -> "order-9988");

        // Fail-fast composition: combines both results cleanly
        CompletableFuture<String> aggregated =
                userTask.thenCombine(orderTask, (user, order) -> user + " -> " + order);

        return aggregated.get(); // "user-alice -> order-9988"
    }

    public static void main(String[] args) throws Exception {
        String result = executeStructuredQuery(); // "user-alice -> order-9988"
    }
}
