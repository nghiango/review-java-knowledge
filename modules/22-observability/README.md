# Module 22 — Observability

This module covers backend observability across logs, metrics, and traces in modern Java/Spring Boot applications:
structured logging, Mapped Diagnostic Context (MDC), correlation ID propagation across asynchronous thread boundaries,
Micrometer Observation API, OpenTelemetry tracing bridge, Prometheus metric registries, preventing cardinality explosion,
error tracking, latency distribution histograms (p95/p99/p999 vs averages), and Actuator production monitoring.

The canonical prose, architecture diagrams, interview Q&A, and operational runbooks live in the documentation:

👉 **[Observability Documentation](../../docs/topics/observability/index.md)**

## Broken Review Examples

This module provides 5 realistic code review targets under `broken-examples/`:

1. `logging-secrets-pii/` — Exposing plain-text passwords, authentication tokens, and credit card numbers in application logs.
2. `missing-correlation-id-async/` — Submitting tasks to background `ExecutorService` pools without propagating MDC trace contexts.
3. `high-cardinality-metric-tags/` — Tagging Micrometer counters/timers with unbounded runtime attributes (user UUIDs, emails), causing memory explosion.
4. `swallowed-exceptions-observability/` — Catching exceptions and logging generic warnings without error tags or causal stack traces.
5. `missing-latency-histogram/` — Measuring downstream API latency with simple duration averages rather than SLA percentile histograms.
