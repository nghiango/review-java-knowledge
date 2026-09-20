# Observability Solutions

This section explains the production-grade implementations for the 5 broken review examples, detailing architectural rationale, concurrency safety, and operational trade-offs.

---

<a id="1-sanitizing-logs-and-masking-sensitive-data"></a>
<a id="sanitizing-logs-and-masking-sensitive-data"></a>
## 1. Sanitizing Logs and Masking Sensitive Data

### Implementation Comparison

=== "Broken"
    ```java
    --8<-- "modules/22-observability/broken-examples/logging-secrets-pii/UserAuthenticationLogger.java"
    ```

=== "Correct"
    ```java
    --8<-- "modules/22-observability/src/main/java/lab/observability/loggingsecrets/CorrectUserAuthenticationLogger.java"
    ```

### Why the Solution Works

1. **Complete Credential Omission**: Passwords and raw secret values are excluded from method arguments and log statements entirely. Only non-sensitive authentication status (`success=true|false`) and client network origins are logged.
2. **Token Truncation**: Bearer tokens are masked to retain only a small, non-replayable footprint (`eyJh...7890`), allowing operators to verify that a token was issued without exposing the credential to log viewers.
3. **PCI-DSS Compliance**: Credit card numbers are sanitized to mask the first 12 digits, exposing only the last 4 digits (`****-****-****-1234`). CVVs are strictly prohibited from log parameters.
4. **Parameterized Placeholders**: Using SLF4J `{}` placeholders avoids eager string allocation and prevents log-forging attacks via injected carriage return / line feed (`\r\n`) sequences.

### Trade-offs
- Support teams cannot inspect the exact raw token in logs when a customer reports authentication rejection; inspection requires user-prompted debug sessions or token introspection endpoints.

---

<a id="2-propagating-mdc-across-asynchronous-threads"></a>
<a id="propagating-mdc-across-asynchronous-threads"></a>
## 2. Propagating MDC Across Asynchronous Threads

### Implementation Comparison

=== "Broken"
    ```java
    --8<-- "modules/22-observability/broken-examples/missing-correlation-id-async/AsyncOrderAuditService.java"
    ```

=== "Correct"
    ```java
    --8<-- "modules/22-observability/src/main/java/lab/observability/correlationasync/CorrectAsyncOrderAuditService.java"
    ```

### Why the Solution Works

1. **Snapshotting Parent Context**: Before handing off work to the background thread pool, `MDC.getCopyOfContextMap()` snapshots the active thread's diagnostic map (including `correlationId`, `traceId`, and `userId`).
2. **Context Restoration**: Upon thread execution, `MDC.setContextMap(contextMap)` populates the worker thread's `ThreadLocal` storage, ensuring all background log statements contain identical correlation identifiers.
3. **Guaranteed Cleanup**: Wrapping execution in a `try ... finally { MDC.clear(); }` block ensures that when the pooled thread finishes and returns to the worker queue, its MDC map is cleared, eliminating cross-tenant context leakage.

### Trade-offs
- Adds a small heap allocation per task submission to snapshot the map. In high-frequency executor pools ($> 100,000\text{ tasks/sec}$), consider using Micrometer Context Propagation with reusable object holders.

---

<a id="3-preventing-high-cardinality-metric-tag-explosion"></a>
<a id="preventing-high-cardinality-metric-tag-explosion"></a>
## 3. Preventing High-Cardinality Metric Tag Explosion

### Implementation Comparison

=== "Broken"
    ```java
    --8<-- "modules/22-observability/broken-examples/high-cardinality-metric-tags/OrderPaymentMetricsService.java"
    ```

=== "Correct"
    ```java
    --8<-- "modules/22-observability/src/main/java/lab/observability/cardinalitytags/CorrectOrderPaymentMetricsService.java"
    ```

### Why the Solution Works

1. **Bounded Dimensional Categories**: The metric tags are restricted to fixed, low-cardinality categorical enums: `payment_method` (`CARD`, `PAYPAL`, `BANK_TRANSFER`, `OTHER`) and `status` (`SUCCESS`, `FAILED`). Total time-series count is capped at $4 \times 2 = 8$ series, guaranteeing minimal memory usage.
2. **Separation of Concerns**: Unbounded entity identifiers (`orderId`, `userId`, `emailHash`) are routed to structured JSON logs and distributed tracing span tags, where high-cardinality attributes can be indexed efficiently without bloating Prometheus TSDB memory.
3. **Scrape Reliability**: The Prometheus scraper endpoint (`/actuator/prometheus`) formats and transmits only a few kilobytes of text per scrape, eliminating scrape timeouts and CPU spikes.

### Trade-offs
- Dashboards in Grafana cannot plot payment metrics filtered by an individual customer's email; querying individual customer activity must be performed in Elasticsearch or Datadog log search.

---

<a id="4-full-causal-exception-logging-and-symmetric-metrics"></a>
<a id="full-causal-exception-logging-and-symmetric-metrics"></a>
## 4. Full Causal Exception Logging and Symmetric Metrics

### Implementation Comparison

=== "Broken"
    ```java
    --8<-- "modules/22-observability/broken-examples/swallowed-exceptions-observability/InventoryReconciliationService.java"
    ```

=== "Correct"
    ```java
    --8<-- "modules/22-observability/src/main/java/lab/observability/swallowedexceptions/CorrectInventoryReconciliationService.java"
    ```

### Why the Solution Works

1. **Preserving Causal Stack Traces**: Passing the `Throwable e` as the final unformatted argument (`log.error("Failed ...", sku, delta, e)`) instructs SLF4J to append the complete multiline stack trace, class name, and line numbers to the log event.
2. **Symmetric Failure Metrics**: When an error occurs, the failure counter `inventory.reconcile.total` is incremented with `status="FAILED"` and `exception=e.getClass().getSimpleName()`. This immediately triggers error-rate threshold alerts in Grafana.
3. **Explicit Structured Result**: Instead of returning a misleading `false`, the method returns a `ReconciliationResult` carrying the failure description, or propagates a domain exception to ensure transactional rollbacks.

### Trade-offs
- Logging full stack traces increases log storage volume during catastrophic downstream outages. Mitigate with Logback deduplicating filters or rate limiters.

---

<a id="5-accurate-percentiles-and-slo-histograms"></a>
<a id="accurate-percentiles-and-slo-histograms"></a>
## 5. Accurate Percentiles and SLO Histograms

### Implementation Comparison

=== "Broken"
    ```java
    --8<-- "modules/22-observability/broken-examples/missing-latency-histogram/CheckoutLatencyTracker.java"
    ```

=== "Correct"
    ```java
    --8<-- "modules/22-observability/src/main/java/lab/observability/latencyhistogram/CorrectCheckoutLatencyTracker.java"
    ```

### Why the Solution Works

1. **Quantile Tail Visibility**: Configuring `publishPercentiles(0.5, 0.95, 0.99, 0.999)` exposes median, p95, p99, and p999 tail latency distributions, immediately highlighting latency spikes, disk stalls, and GC pauses masked by arithmetic averages.
2. **Service Level Objective (SLO) Buckets**: Setting explicit `serviceLevelObjectives(...)` exports cumulative histogram bucket counters (`checkout_duration_seconds_bucket{le="0.5"}`). Prometheus can query exact error budgets:
   ```promql
   sum(rate(checkout_duration_seconds_bucket{le="0.5"}[5m]))
   / sum(rate(checkout_duration_seconds_count[5m]))
   ```
3. **Decay-Biased Histograms**: Micrometer's internal ring-buffer histogram discards old samples over time, ensuring percentiles reflect current real-time performance rather than historical lifetime aggregates.

### Trade-offs
- Histogram buckets increase the number of exported time series per timer. Allocate histogram buckets deliberately on critical customer-facing transactions.

---

## Related

- [Concepts](concepts.md) — Metric types, structured logs, and W3C TraceContext
- [Code Review](code-review.md) — Review targets and issue analysis
- [Tests](tests.md) — Unit and integration tests for observability components
- [Production](production.md) — Real-world incident runbooks and alerting rules
