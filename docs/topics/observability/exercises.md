# Observability Exercises

Hands-on exercises to master asynchronous context propagation, metric cardinality control, and high-precision SLO latency tracking.

## Exercise 1: Asynchronous MDC Task Decorator with Context Leak Prevention

### Problem
In a multithreaded order processing microservice, tasks dispatched to background `ExecutorService` thread pools lose their originating `correlationId` and `traceId`. Furthermore, uncleaned thread-local data leaks across pooled tasks, polluting log aggregators with cross-tenant data.

Implement an asynchronous task decorator that captures the calling thread's diagnostic context, restores it on the worker thread, and guarantees cleanup upon task completion.

### Requirements
1. Capture `MDC.getCopyOfContextMap()` on the parent thread prior to task submission.
2. Apply the captured context map inside the worker thread prior to running business logic.
3. Guarantee that `MDC.clear()` executes in a `finally` block when the worker thread returns to the pool.
4. Support empty or null parent MDC states without throwing `NullPointerException`.

??? tip "Reveal solution"
    ```java
    --8<-- "modules/22-observability/src/main/java/lab/observability/correlationasync/CorrectAsyncOrderAuditService.java"
    ```

---

## Exercise 2: High-Precision Latency Histogram with SLO Boundaries

### Problem
Design and implement a checkout latency tracker that records payment processing response times. The business requires monitoring 99.9% service level objectives (SLOs) and detecting tail latency outliers (p99 and p999) caused by GC pauses and database lock contention.

### Requirements
1. Use Micrometer `Timer.builder()` to declare a production-ready latency metric.
2. Configure quantile percentiles for median (0.5), p95 (0.95), p99 (0.99), and p999 (0.999).
3. Publish percentile histograms for Prometheus `histogram_quantile()` queries.
4. Define explicit Service Level Objective (SLO) duration boundaries (e.g. 50ms, 100ms, 250ms, 500ms, 1s, 2s).

??? tip "Reveal solution"
    ```java
    --8<-- "modules/22-observability/src/main/java/lab/observability/latencyhistogram/CorrectCheckoutLatencyTracker.java"
    ```

---

## Related

- [Concepts](concepts.md) — Telemetry foundations and metric data models
- [Code Review](code-review.md) — Reviewing broken observability patterns
- [Solutions](solutions.md) — Production implementations and trade-offs
- [Tests](tests.md) — Testing metrics and MDC propagation
- [Production](production.md) — Production incident triage and alerting rules
