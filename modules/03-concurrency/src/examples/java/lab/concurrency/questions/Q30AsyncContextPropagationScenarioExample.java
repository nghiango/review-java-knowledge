package lab.concurrency.questions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class Q30AsyncContextPropagationScenarioExample {
    private Q30AsyncContextPropagationScenarioExample() {}

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    // Propagating wrapper that captures caller thread context and restores it on the executor worker
    public static <T> Supplier<T> withContext(Supplier<T> supplier) {
        String captured = TRACE_ID.get();
        return () -> {
            String previous = TRACE_ID.get();
            TRACE_ID.set(captured);
            try {
                return supplier.get();
            } finally {
                if (previous != null) {
                    TRACE_ID.set(previous);
                } else {
                    TRACE_ID.remove();
                }
            }
        };
    }

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        TRACE_ID.set("trace-abc-123");

        // 1. Unwrapped stage: ThreadLocal is NOT propagated; returns null!
        CompletableFuture<String> stageWithoutContext = CompletableFuture.supplyAsync(() -> {
            return TRACE_ID.get(); // null (executed on pool worker thread without context!)
        }, pool);

        // 2. Wrapped stage: captures and restores context across thread boundary
        CompletableFuture<String> stageWithContext = CompletableFuture.supplyAsync(
            withContext(() -> TRACE_ID.get()), // "trace-abc-123"
            pool
        );

        String unwrappedVal = stageWithoutContext.get(); // null
        String wrappedVal = stageWithContext.get(); // "trace-abc-123"

        pool.shutdown();
        TRACE_ID.remove();
    }
}
