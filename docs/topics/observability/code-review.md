# Observability Code Review

This section presents 5 realistic pull-request code reviews focusing on data privacy in logs, asynchronous context propagation, metric cardinality explosion, swallowed exceptions, and latency histograms.

---

<a id="logging-secrets-pii"></a>
## 1. Logging Secrets and PII

### Context
An authentication and billing audit component logs security events and payment transactions. An engineer authored `UserAuthenticationLogger.java` to record user login attempts, session issuance, and payment processing for operational visibility.

### Review Target
```java
--8<-- "modules/22-observability/broken-examples/logging-secrets-pii/UserAuthenticationLogger.java"
```

### Review Prompt
Review `UserAuthenticationLogger.java` with focus on:
- Plaintext exposure of credentials, passwords, tokens, and cryptographic secrets
- Personally Identifiable Information (PII) and Payment Card Industry (PCI-DSS) violations
- Log injection risks from unsanitized string concatenation
- Use of parameterized logging vs eager string formatting

??? warning "Reveal issues"
    ### Security issue: Plaintext passwords and bearer tokens logged in clear text

    #### Problem
    The logger prints raw passwords and authentication bearer tokens into log events.

    #### Why it happens
    The developer logged method arguments for debugging purposes without considering that log events are shipped to centralized indices readable by all developers and contractors.

    #### Production impact
    ```text
    User logs in with credentials
    → Plaintext password written to application log file
    → Shipped to centralized Elasticsearch / Datadog
    → Any employee or compromised log service token can harvest user credentials
    → Total account takeover and regulatory breach
    ```

    #### Why the solution works
    Never log raw credentials. Log only the username and whether authentication succeeded. Mask bearer tokens to small footprints or truncated prefixes.

    #### Trade-offs
    Debugging authentication issues requires inspecting client handshake errors rather than viewing raw password strings.

    #### How to detect it
    Log monitoring DLP tools (Data Loss Prevention) flag regex matches for JWT tokens or password fields.

    #### Interview follow-up
    > What are the PCI-DSS requirements regarding primary account numbers (PAN) and CVVs in logs?

    #### Related
    - [Concepts — Sensitive Data Privacy & PII Redaction](concepts.md#3-sensitive-data-privacy-pii-redaction)

[View Solution & Correct Implementation](solutions.md#1-sanitizing-logs-and-masking-sensitive-data)

---

<a id="missing-correlation-id-async"></a>
## 2. Missing Correlation ID Across Threads

### Context
An order fulfillment service offloads compliance auditing to an asynchronous `ExecutorService` thread pool. An engineer authored `AsyncOrderAuditService.java` to asynchronously record audit events while logging diagnostics.

### Review Target
```java
--8<-- "modules/22-observability/broken-examples/missing-correlation-id-async/AsyncOrderAuditService.java"
```

### Review Prompt
Review `AsyncOrderAuditService.java` with focus on:
- Transfer of Mapped Diagnostic Context (MDC) across asynchronous thread boundaries
- Cross-tenant context leakage in pooled worker threads
- Exception safety and cleanup guarantees (`MDC.clear()`)
- Correlation ID persistence across distributed log aggregators

??? warning "Reveal issues"
    ### Observability issue: MDC context lost across asynchronous thread pool boundaries

    #### Problem
    `CompletableFuture.runAsync()` dispatches a task to an `Executor` without copying the parent thread's MDC context map.

    #### Why it happens
    SLF4J MDC is backed by Java `ThreadLocal`. When a background worker thread executes the runnable, its thread-local map is empty by default.

    #### Production impact
    ```text
    Incoming HTTP request sets correlationId="corr-123"
    → Task dispatched to thread pool worker
    → Background logs show correlationId=null
    → Searching Kibana for "corr-123" returns only 2 out of 10 log statements
    → Impossible to correlate async background failures with the originating request
    ```

    #### Why the solution works
    Capturing `MDC.getCopyOfContextMap()` before submitting to the executor and calling `MDC.setContextMap()` inside the worker thread preserves the context. A `finally { MDC.clear(); }` block ensures pooled threads do not leak stale IDs to future tasks.

    #### Trade-offs
    Allocates a small map snapshot per asynchronous task submission.

    #### How to detect it
    Log queries for asynchronous job failures return empty or mismatched correlation IDs.

    #### Interview follow-up
    > How does Spring's `TaskDecorator` automate MDC propagation across `@Async` thread pools?

    #### Related
    - [Concepts — Structured Logging & Mapped Diagnostic Context (MDC)](concepts.md#2-structured-logging-mapped-diagnostic-context-mdc)
    - [Internals — Logback Appender Architecture & Async Buffering](internals.md#2-logback-appender-architecture-async-buffering)

[View Solution & Correct Implementation](solutions.md#2-propagating-mdc-across-asynchronous-threads)

---

<a id="high-cardinality-metric-tags"></a>
## 3. High-Cardinality Metric Tags

### Context
A high-volume e-commerce checkout service records transaction volume using Micrometer metrics. An engineer authored `OrderPaymentMetricsService.java` to track processed payments for business dashboards.

### Review Target
```java
--8<-- "modules/22-observability/broken-examples/high-cardinality-metric-tags/OrderPaymentMetricsService.java"
```

### Review Prompt
Review `OrderPaymentMetricsService.java` with focus on:
- Inclusion of high-cardinality attributes (UUIDs, user IDs, emails, timestamps) as metric tags
- Metric registry memory growth and heap starvation
- Scraping performance degradation on Prometheus endpoints (`/actuator/prometheus`)
- Appropriate separation of metrics vs structured logs vs distributed traces

??? warning "Reveal issues"
    ### Performance issue: High-cardinality tags cause MetricRegistry memory explosion

    #### Problem
    The method tags a Micrometer Counter with dynamic runtime attributes: `order_id`, `user_id`, and `customer_email`.

    #### Why it happens
    The developer wanted to filter Grafana dashboards by specific order or user identifiers without understanding how time-series databases index dimensions.

    #### Production impact
    ```text
    500,000 customers place orders
    → 500,000 unique Meter instances created in MeterRegistry
    → Internal ConcurrentHashMap balloons to gigabytes of heap memory
    → Major GC pauses spike to multiple seconds
    → Prometheus scrapes /actuator/prometheus and times out after 10s
    → Application crashes with java.lang.OutOfMemoryError: Java heap space
    ```

    #### Why the solution works
    Restricting metric tags to low-cardinality categorical dimensions (e.g. `payment_method: CARD|PAYPAL`, `status: SUCCESS|FAILED`) bounds total metric time series to $< 100$ combinations. Specific order IDs are logged in structured JSON logs or trace spans instead.

    #### Trade-offs
    Dashboards cannot filter Prometheus metrics by an individual customer's email; customer queries must be performed in log aggregators.

    #### How to detect it
    Metric `jvm.memory.used` continuously climbs in Tenured Space; Prometheus reports `scrape_duration_seconds > 10`.

    #### Interview follow-up
    > What is the mathematical definition of metric cardinality?

    #### Related
    - [Concepts — Metric Cardinality Explosion](concepts.md#5-metric-cardinality-explosion)
    - [Internals — Prometheus Scraper & OpenMetrics Exposition](internals.md#3-prometheus-scraper-openmetrics-exposition)

[View Solution & Correct Implementation](solutions.md#3-preventing-high-cardinality-metric-tag-explosion)

---

<a id="swallowed-exceptions-observability"></a>
## 4. Swallowed Exceptions in Observability

### Context
An inventory synchronization pipeline reconciles stock adjustments with an ERP store. An engineer authored `InventoryReconciliationService.java` to apply stock adjustments and record business metrics.

### Review Target
```java
--8<-- "modules/22-observability/broken-examples/swallowed-exceptions-observability/InventoryReconciliationService.java"
```

### Review Prompt
Review `InventoryReconciliationService.java` with focus on:
- Preservation of exception causality and full stack traces in log statements
- Symmetric tracking of failure metrics alongside success counters
- Silent error suppression masking data corruption or downstream failures
- Structured context attribution on error events

??? warning "Reveal issues"
    ### Observability issue: Exception stack trace dropped by logging only `e.getMessage()`

    #### Problem
    The catch block executes `log.error("Failed to reconcile stock for " + sku + ": " + e.getMessage())` and only increments a success metric.

    #### Why it happens
    The developer used string concatenation instead of passing the `Throwable` object as the final parameter, and placed metric increments only on the happy path.

    #### Production impact
    ```text
    Database connection drops during reconciliation
    → Catch block logs: "Failed to reconcile stock for CAT-123: null" (NPE has null message!)
    → Zero stack trace, zero line number, zero root cause in logs
    → No failure metric incremented → Error rate dashboard shows 0.0% failure rate
    → Stock drift accumulates silently across thousands of items undetected
    ```

    #### Why the solution works
    Passing the exception instance `e` as the final argument (`log.error("Failed sku={}", sku, e)`) preserves the complete stack trace. Incrementing `inventory.reconcile.total` with `status="FAILED"` and `exception=e.getClass().getSimpleName()` ensures errors appear immediately on Grafana dashboards.

    #### Trade-offs
    Increased log volume during downstream outages; mitigable via log rate limiters.

    #### How to detect it
    Log files contain error messages without corresponding multiline stack traces; error rate dashboards show 0 errors during customer complaint spikes.

    #### Interview follow-up
    > Why is logging `e.getMessage()` dangerous when handling `NullPointerException`?

    #### Related
    - [Concepts — Structured Logging & Mapped Diagnostic Context (MDC)](concepts.md#2-structured-logging-mapped-diagnostic-context-mdc)
    - [Questions — Q23 Incident Silent Failure](questions.md#q23-incident-asynchronous-payment-processing-fails-silently-for-10000-customers-while-grafana-error-dashboards-report-0-failure-rate-how-do-you-identify-the-defect-and-remediate)

[View Solution & Correct Implementation](solutions.md#4-full-causal-exception-logging-and-symmetric-metrics)

---

<a id="missing-latency-histogram"></a>
## 5. Missing Latency Histogram and Tail Outliers

### Context
A checkout processing service measures response times for payment completion. An engineer authored `CheckoutLatencyTracker.java` to compute latency metrics and expose average execution times.

### Review Target
```java
--8<-- "modules/22-observability/broken-examples/missing-latency-histogram/CheckoutLatencyTracker.java"
```

### Review Prompt
Review `CheckoutLatencyTracker.java` with focus on:
- Flaws of arithmetic mean/average latency measurements under skewed distributions
- Visibility of tail latency (p95, p99, p999) and GC pauses
- Configuration of Micrometer `Timer` with histogram buckets and percentiles
- Service Level Objective (SLO) boundary enforcement

??? warning "Reveal issues"
    ### Observability issue: Arithmetic mean latency obscures severe tail latency spikes

    #### Problem
    The service measures performance using an arithmetic average (`totalDurationMs / totalCount`) and an unconfigured `Timer`.

    #### Why it happens
    The developer assumed that average response time is sufficient to gauge customer satisfaction.

    #### Production impact
    ```text
    1,000 customers complete checkout:
    - 990 requests complete in 20ms
    - 10 requests hang on database locks for 15,000ms
    → Average latency reported: ~169ms (Dashboard appears completely green!)
    → Reality: 1% of customers suffered a 15-second hang and abandoned their carts
    ```

    #### Why the solution works
    Configuring `Timer.builder("checkout.duration").publishPercentiles(0.5, 0.95, 0.99, 0.999).serviceLevelObjectives(...)` exports exact percentile distributions and histogram buckets, exposing tail latency outliers to SRE alerts.

    #### Trade-offs
    Percentile histograms allocate additional internal decay buckets in memory, adding minor CPU and memory overhead compared to raw counters.

    #### How to detect it
    Customer complaints about checkout timeouts while Grafana average latency graphs remain well within acceptable thresholds.

    #### Interview follow-up
    > How does Prometheus calculate `histogram_quantile(0.99, ...)` using bucketed latency histograms?

    #### Related
    - [Concepts — Percentiles vs Arithmetic Averages](concepts.md#6-percentiles-vs-arithmetic-averages)
    - [Questions — Q13 Percentiles vs Average Latency](questions.md#q13-why-does-arithmetic-average-latency-mislead-engineering-teams-and-how-do-percentiles-solve-this)

[View Solution & Correct Implementation](solutions.md#5-accurate-percentiles-and-slo-histograms)

---

## Related

- [Solutions](solutions.md) — Detailed code diffs and architectural fixes
- [Tests](tests.md) — Testing metrics and MDC propagation
- [Questions](questions.md) — Comprehensive interview questions on observability
- [Production](production.md) — Production incident triage and alerting rules
