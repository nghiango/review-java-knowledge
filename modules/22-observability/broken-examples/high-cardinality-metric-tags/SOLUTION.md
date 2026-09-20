# Solution — High-Cardinality Metric Tags

## Annotated code

```java
package lab.observability.broken.cardinalitytags;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class OrderPaymentMetricsService {

    private final MeterRegistry meterRegistry;

    public OrderPaymentMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordPaymentAttempt(String orderId, String userId, String email, String method, boolean success) {
        // Performance issue: Adding unbounded, unique tag values (orderId, userId, email) causes metric cardinality explosion.
        // Memory issue: In Micrometer, each distinct tag set creates an immortal Meter instance in the MeterRegistry.
        // Observability issue: Prometheus cannot index millions of dynamic series without crashing scrapers or TSDB storage.
        meterRegistry.counter(
                "payments.processed.total",
                "order_id", orderId,
                "user_id", userId,
                "customer_email", email,
                "payment_method", method,
                "status", success ? "SUCCESS" : "FAILED"
        ).increment();
    }
}
```

## Issue list

### Performance issue: High-cardinality tags cause MetricRegistry memory explosion

- **Location:** `OrderPaymentMetricsService.java:18`
- **Description:** Adding `order_id` (UUID), `user_id`, and `customer_email` creates millions of unique meter definitions.
- **Impact:** Each unique tag set allocates a new `Counter` object retained in `MeterRegistry`'s internal concurrent hash map. Under high traffic, this causes unbounded heap growth, severe Garbage Collection pauses, and eventual JVM `OutOfMemoryError`.
- **Remediation:** Remove high-cardinality tags from metric dimensions. Keep metrics strictly to low-cardinality categorical dimensions (`payment_method`, `status`). Record specific transaction identifiers (`order_id`, `user_id`) in structured logs and distributed tracing spans instead.

### Observability issue: Scraper timeouts and TSDB database crash

- **Location:** `OrderPaymentMetricsService.java:18`
- **Description:** When Prometheus scrapes `/actuator/prometheus`, generating plaintext output for 1,000,000 meters takes tens of seconds and megabytes of bandwidth.
- **Impact:** Prometheus scrape timeouts (`context deadline exceeded`), lost telemetry, and TSDB index corruption.
- **Remediation:** Restrict tag values to small, predefined enums or categories ($< 100$ distinct combinations per meter).

## Correct implementation

See [`lab.observability.cardinalitytags.CorrectOrderPaymentMetricsService`](../../src/main/java/lab/observability/cardinalitytags/CorrectOrderPaymentMetricsService.java).

Detailed discussion in [Solutions](../../../docs/topics/observability/solutions.md#3-preventing-high-cardinality-metric-tag-explosion).
