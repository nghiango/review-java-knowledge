package lab.observability.correlationasync;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class CorrectAsyncOrderAuditService {

    private static final Logger log = LoggerFactory.getLogger(CorrectAsyncOrderAuditService.class);

    private final Executor asyncExecutor;

    public CorrectAsyncOrderAuditService(Executor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    public CompletableFuture<Void> auditOrderPlacement(String orderId, String userId) {
        MDC.put("correlationId", "corr-" + orderId);
        log.info("Dispatching async audit for orderId={} userId={}", orderId, userId);

        // Capture parent thread MDC snapshot prior to submitting to asynchronous worker pool
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return CompletableFuture.runAsync(
                () -> {
                    // Restore MDC context on worker thread
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    try {
                        log.info(
                                "Executing background compliance audit check for orderId={}",
                                orderId);
                        performAuditCheck(orderId, userId);
                    } finally {
                        // Strict cleanup: clear MDC when worker thread returns to pool to prevent
                        // dirty state leakage
                        MDC.clear();
                    }
                },
                asyncExecutor);
    }

    private void performAuditCheck(String orderId, String userId) {
        log.debug("Verification passed for orderId={} userId={}", orderId, userId);
    }
}
