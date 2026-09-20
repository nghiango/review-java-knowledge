package lab.observability.broken.correlationasync;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class AsyncOrderAuditService {

    private static final Logger log = LoggerFactory.getLogger(AsyncOrderAuditService.class);

    private final Executor asyncExecutor;

    public AsyncOrderAuditService(Executor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    public CompletableFuture<Void> auditOrderPlacement(String orderId, String userId) {
        MDC.put("correlationId", "corr-" + orderId);
        log.info("Dispatching async audit for order {} user {}", orderId, userId);

        // Submitting to executor directly without capturing MDC
        return CompletableFuture.runAsync(() -> {
            // Correlation ID is lost because ThreadLocal MDC does not transfer to pool worker thread
            log.info("Executing background compliance audit check for order {}", orderId);
            performAuditCheck(orderId, userId);
            // Missing MDC.clear() on thread return, polluting subsequent tasks on this pooled thread
        }, asyncExecutor);
    }

    private void performAuditCheck(String orderId, String userId) {
        log.debug("Verification passed for order {}", orderId);
    }
}
