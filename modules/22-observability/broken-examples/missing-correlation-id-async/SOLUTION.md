# Solution — Missing Correlation ID Across Threads

## Annotated code

```java
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

        // Observability issue: ThreadLocal MDC context does not propagate automatically to executor worker threads.
        // Concurrency issue: Uncleaned MDC in pooled threads leaks previous tenant correlation IDs to subsequent tasks.
        return CompletableFuture.runAsync(() -> {
            // Correlation ID is null here in logs, breaking distributed tracing across asynchronous boundaries
            log.info("Executing background compliance audit check for order {}", orderId);
            performAuditCheck(orderId, userId);
            // Resource leak issue: Missing try-finally MDC.clear() leaves dirty context on reusable worker thread
        }, asyncExecutor);
    }

    private void performAuditCheck(String orderId, String userId) {
        log.debug("Verification passed for order {}", orderId);
    }
}
```

## Issue list

### Observability issue: MDC context lost across asynchronous thread pool boundaries

- **Location:** `AsyncOrderAuditService.java:23`
- **Description:** SLF4J MDC is backed by `ThreadLocal`. When a task is dispatched to an `Executor`, the child worker thread starts with an empty MDC map.
- **Impact:** Log lines emitted by background threads lack `correlationId`, `traceId`, or `spanId`, making it impossible to correlate asynchronous background operations with the originating HTTP request in Kibana or Datadog.
- **Remediation:** Capture `MDC.getCopyOfContextMap()` before submitting the task, set it in the worker thread, and restore/clear it in a `finally` block, or use Spring's `TaskDecorator`.

### Concurrency issue: Thread pool reuse leaks dirty MDC state across requests

- **Location:** `AsyncOrderAuditService.java:27`
- **Description:** If a task sets values in MDC on a pooled thread without calling `MDC.clear()` in a `finally` block, subsequent unrelated tasks executed on that thread inherit the prior task's correlation ID.
- **Impact:** Cross-tenant log pollution where Request B's logs appear tagged with Request A's customer or order ID, misleading production incident triage.
- **Remediation:** Enforce strict cleanup via `try ... finally { MDC.clear(); }`.

## Correct implementation

See [`lab.observability.correlationasync.CorrectAsyncOrderAuditService`](../../src/main/java/lab/observability/correlationasync/CorrectAsyncOrderAuditService.java).

Detailed discussion in [Solutions](../../../docs/topics/observability/solutions.md#2-propagating-mdc-across-asynchronous-threads).
