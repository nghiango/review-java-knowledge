package lab.concurrency.lockordering;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class AuditNotificationClient {
    private final Executor asyncExecutor;

    public AuditNotificationClient(Executor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    public CompletableFuture<Void> notifyTransferAsync(String message) {
        return CompletableFuture.runAsync(
                () -> {
                    // Simulated non-blocking external notification emission
                },
                asyncExecutor);
    }
}
