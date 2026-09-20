# Observability in Production

Operating observability infrastructure at scale requires strict safeguards against metric cardinality explosion, log buffer saturation, and silent failure concealment.

## 1. Incident Walkthrough: The Prometheus Cardinality Explosion

### Incident Profile
- **Severity**: P1 Outage (Monitoring Blindness & Container OOM)
- **Duration**: 50 minutes
- **Impact**: Prometheus TSDB server crashed; all production Kubernetes pods ran out of heap space during a flash sale; alerting went completely dark.

### The Trigger & Cascading Failure

```mermaid
sequenceDiagram
    autonumber
    actor Shoppers as 500,000 Flash Sale Shoppers
    participant Service as Order Service (10 Pods)
    participant Prometheus as Prometheus Scraper & TSDB
    participant Alertmanager as Alertmanager / PagerDuty

    Shoppers->>Service: POST /orders (High Volume Traffic)
    Note over Service: Code tags 'orders.placed' with user_id and email!
    Note over Service: 500,000 unique meters allocated in MeterRegistry
    Note over Service: Heap utilization hits 98%; Full GC pauses reach 8 seconds!
    Prometheus->>Service: GET /actuator/prometheus (Periodic 15s Scrape)
    Note over Service: Actuator attempts to serialize 500,000 lines of OpenMetrics text (80 MB!)
    Service--xPrometheus: HTTP Scrape Times Out (> 10s deadline)
    Note over Prometheus: Scraper marks all 10 pods as DOWN (up == 0)
    Note over Prometheus: TSDB memory explodes while indexing 500,000 new series
    Note over Prometheus: Prometheus server crashes with OOMKilled!
    Alertmanager--xAlertmanager: Alerts stop evaluating (Metrics Pipeline Dead)
    Note over Service: Application pods crash with java.lang.OutOfMemoryError: Java heap space
```

### Root Cause Analysis
1. **Dynamic High-Cardinality Tags**: A newly released marketing tracking feature tagged `orders.placed` with `user_id` and `customer_email`.
2. **Tenured Heap Exhaustion**: Each unique tag set allocated a new `Counter` instance in `MeterRegistry`'s internal map, consuming 3.5 GB of JVM heap memory.
3. **Scraper Collapse**: Serializing 500,000 metric lines exceeded Actuator's memory and CPU budget, timing out Prometheus scrapes and blinding the operations team to concurrent payment failures.

### Remediation & Post-Mortem Actions
- **Emergency Filter**: Applied an immediate Spring Boot configuration property via runtime ConfigMap:
  ```yaml
  management:
    metrics:
      filter:
        deny:
          names: orders.placed
  ```
- **Code Refactor**: Stripped `user_id` and `customer_email` from the metric. Customer identifiers were rerouted to structured JSON log events.
- **Enforced MeterFilter Limit**: Added a global safeguard in the application startup configuration:
  ```java
  @Bean
  public MeterFilter meterFilterLimit() {
      return MeterFilter.maximumAllowableTags("orders.placed", 100, MeterFilter.deny());
  }
  ```

---

## 2. Incident Walkthrough: The Silent Payment Failure Outage

### Incident Profile
- **Severity**: P1 Silent Revenue Loss
- **Duration**: 3.5 hours
- **Impact**: 14,000 customer payment attempts failed; executive dashboards reported a 0.00% error rate; discovery occurred only after social media customer complaints.

### Root Cause Analysis
- An upstream payment gateway updated its TLS cipher suite, causing `SSLHandshakeException` on checkout requests.
- The checkout service caught `Exception`, logged `log.error("Payment error: " + e.getMessage())`, and returned `false`.
- The failure counter was never incremented because errors were swallowed.
- Furthermore, because `e.getMessage()` was logged without the `Throwable` instance, the log message contained `"Payment error: null"`, concealing the SSL handshake cause.

### Remediation
- Implemented symmetric metrics tracking (`payments.processed.total` tagged with `status="SUCCESS|FAILED"` and `exception=e.getClass().getSimpleName()`).
- Updated SLF4J log calls to always pass the `Throwable` object as the final parameter.
- Configured synthetic canary transactions executing end-to-end payment simulations every 60 seconds.

---

## 3. Production Telemetry & Alerts

Monitor the following Prometheus metrics for application and infrastructure health:

### Critical Telemetry Metrics

| Metric | Type | Purpose | Alert Threshold |
|---|---|---|---|
| `jvm.memory.used{area="heap"}` | Gauge | JVM Heap memory consumption | $> 85\%$ of `jvm.memory.max` |
| `scrape_duration_seconds` | Gauge | Time taken to scrape `/actuator/prometheus` | $> 5.0\text{s}$ |
| `scrape_samples_scraped` | Gauge | Total distinct time-series exported per pod | $> 2,000$ (indicates cardinality leak) |
| `executor.active` | Gauge | Active threads in application worker pools | Equal to `executor.pool.size` |
| `executor.queue.remaining` | Gauge | Remaining task queue capacity | $< 10\%$ of total queue |

### Prometheus Alerting Rules

```yaml
groups:
  - name: observability-infrastructure-alerts
    rules:
      - alert: PrometheusScrapeTimeoutRisk
        expr: scrape_duration_seconds{job="spring-boot-apps"} > 4.0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Prometheus scrape duration is dangerously high; investigate metric cardinality explosion"

      - alert: MetricCardinalityExplosion
        expr: scrape_samples_scraped{job="spring-boot-apps"} > 5000
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Pod is exporting > 5000 distinct time-series; dynamic tag leak detected"

      - alert: HighErrorRateSloBreach
        expr: |
          (
            sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
            /
            sum(rate(http_server_requests_seconds_count[5m]))
          ) > 0.01
        for: 2m
        labels:
          severity: page
        annotations:
          summary: "HTTP 5xx error rate exceeds 1% of total traffic; burning SLO error budget"
```

---

## 4. Production Readiness Checklist

Before deploying any microservice to production, verify each of the following observability controls:

- [ ] **Structured JSON Logging Enabled**: Logback formatted using Logstash JSON encoder; no raw multiline plaintext in production.
- [ ] **MDC Propagation Across Async Pools**: All `ExecutorService` and `@Async` pools decorated with `TaskDecorator` to preserve correlation IDs and call `MDC.clear()`.
- [ ] **Zero High-Cardinality Tags**: Meters verified in CI to contain no UUIDs, timestamps, user IDs, or unconstrained strings.
- [ ] **Full Causal Exception Logging**: All `log.error()` statements pass the `Throwable` instance as the final parameter.
- [ ] **Symmetric Metric Instrumentation**: Every business operation records both success and failure outcomes with explicit tags.
- [ ] **Percentile & SLO Histograms Active**: Critical HTTP and transaction timers declare explicit SLO buckets and percentiles (p50, p95, p99).
- [ ] **Sensitive Data Masking**: Logback masking rules active for credit card PANs, passwords, and authorization bearer tokens.
- [ ] **Actuator Security Enforced**: Sensitive Actuator endpoints (`/actuator/env`, `/actuator/heapdump`) isolated to internal management ports.

---

## Related

- [Concepts](concepts.md) — Telemetry principles and RED/USE methods
- [Internals](internals.md) — Observation lifecycle and Prometheus scraper architecture
- [Code Review](code-review.md) — Reviewing broken observability code
- [Solutions](solutions.md) — Production-grade implementations and trade-offs
