# Solution — Swallowed Exceptions in Observability

## Annotated code

```java
package lab.observability.broken.swallowedexceptions;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InventoryReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(InventoryReconciliationService.class);

    private final Counter successCounter;
    private final InventoryRepository inventoryRepository;

    public InventoryReconciliationService(MeterRegistry meterRegistry, InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
        this.successCounter = meterRegistry.counter("inventory.reconcile.success");
    }

    public boolean reconcileStock(String sku, int delta) {
        try {
            inventoryRepository.adjustStock(sku, delta);
            successCounter.increment();
            return true;
        } catch (Exception e) {
            // Observability issue: Logging e.getMessage() without passing the Throwable discards the stack trace and root cause.
            // Observability issue: Asymmetric metrics tracking records success but fails to increment an error counter or failure tag.
            // Reliability issue: Returning false without surfacing a domain exception silences critical data drift.
            log.error("Failed to reconcile stock for " + sku + ": " + e.getMessage());
            return false;
        }
    }

    public interface InventoryRepository {
        void adjustStock(String sku, int delta);
    }
}
```

## Issue list

### Observability issue: Exception stack trace dropped by logging only `e.getMessage()`

- **Location:** `InventoryReconciliationService.java:32`
- **Description:** SLF4J methods like `log.error("...", e.getMessage())` treat the message string as a parameter; the `Throwable` object is never passed, so the stack trace is completely omitted from the log output. Furthermore, if `e.getMessage()` is `null` (common with `NullPointerException`), the log prints `"null"`.
- **Impact:** Engineers investigating production incidents see an error string with zero indication of class, line number, or nested root causes.
- **Remediation:** Pass the `Throwable` as the final unformatted argument: `log.error("Failed to reconcile stock for sku={}", sku, e)`.

### Observability issue: Asymmetric metrics hide production failure rate

- **Location:** `InventoryReconciliationService.java:33`
- **Description:** Only successful operations increment a counter; failures are caught and return `false` without incrementing a corresponding failure meter.
- **Impact:** Grafana dashboards calculating error rates or monitoring error alerts show 0 failures, concealing system outages from on-call engineers.
- **Remediation:** Track operations using a unified `Timer` or `Counter` with a `status="success|failure"` tag, or explicitly increment a failure counter.

### Reliability issue: Silent failure suppression masks persistent data drift

- **Location:** `InventoryReconciliationService.java:34`
- **Description:** Returning a boolean `false` suppresses the error from callers and transaction managers.
- **Impact:** Upstream orchestrators continue processing as if the operation completed normally or retry blindly without knowing the failure classification.
- **Remediation:** Propagate an explicit domain exception or structured result.

## Correct implementation

See [`lab.observability.swallowedexceptions.CorrectInventoryReconciliationService`](../../src/main/java/lab/observability/swallowedexceptions/CorrectInventoryReconciliationService.java).

Detailed discussion in [Solutions](../../../docs/topics/observability/solutions.md#4-full-causal-exception-logging-and-symmetric-metrics).
