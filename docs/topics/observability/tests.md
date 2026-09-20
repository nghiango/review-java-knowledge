# Observability Tests

Testing observability components ensures telemetry metrics, log redaction rules, and distributed trace context propagation work reliably without causing production regressions or memory leaks.

## Testing Strategy

1. **In-Memory Metric Verification with `SimpleMeterRegistry`**:
   `SimpleMeterRegistry` stores counters, timers, and gauges in local JVM memory without external network dependencies:
   - Verifies that meters are registered with the expected name and low-cardinality tags.
   - Asserts that dynamic tags (such as order UUIDs or user emails) are never added to the registry.
   - Verifies symmetric error counting when business operations throw exceptions.
2. **Asynchronous MDC Context Propagation Assertions**:
   - Asserts that worker threads executing asynchronous tasks inherit parent `correlationId` and `traceId` values.
   - Proves that pooled threads invoke `MDC.clear()` upon task completion, preventing dirty context leaks.
3. **End-to-End Telemetry Integration Testing**:
   Combines asynchronous executors, metrics services, and structured logging components to prove thread safety, accurate timer counts, and sanitized outputs.

## Test Suite Overview

```bash
# Fast in-process unit tests
./gradlew :modules:22-observability:test

# Telemetry integration tests
./gradlew :modules:22-observability:integrationTest
```

| Test Class | Invariant Proved | Technique |
|---|---|---|
| `CorrectUserAuthenticationLoggerTest` | Passwords, tokens, and credit card numbers are sanitized without throwing exceptions | SLF4J logger verification |
| `CorrectAsyncOrderAuditServiceTest` | Parent MDC context transfers to worker thread and clears on task completion | `ExecutorService` + `ThreadLocal` assertion |
| `CorrectOrderPaymentMetricsServiceTest` | 100 unique order transactions allocate exactly 1 bounded Counter in registry | `SimpleMeterRegistry` meter count assertion |
| `CorrectInventoryReconciliationServiceTest` | Symmetrically tracks success and failure metrics and captures causal exception tags | Mockito + `SimpleMeterRegistry` verification |
| `CorrectCheckoutLatencyTrackerTest` | Timer records duration distributions and exposes high-precision latency | `SimpleMeterRegistry` timer assertions |
| `ObservabilityIntegrationTest` | End-to-end checkout flow emits metrics, logs securely, and preserves async MDC | Multi-threaded component integration |

## Key Test Snippets

### Proving Low-Cardinality Metric Bounding

```java
--8<-- "modules/22-observability/src/test/java/lab/observability/cardinalitytags/CorrectOrderPaymentMetricsServiceTest.java"
```

### Proving Asynchronous MDC Propagation & Cleanup

```java
--8<-- "modules/22-observability/src/test/java/lab/observability/correlationasync/CorrectAsyncOrderAuditServiceTest.java"
```

### Proving End-to-End Telemetry Flow

```java
--8<-- "modules/22-observability/src/integrationTest/java/lab/observability/ObservabilityIntegrationTest.java"
```

## Related

- [Concepts](concepts.md) — Telemetry principles and metric data models
- [Code Review](code-review.md) — Reviewing broken observability PRs
- [Solutions](solutions.md) — Production-grade implementations
- [Production](production.md) — Telemetry runbooks and Prometheus alerting rules
