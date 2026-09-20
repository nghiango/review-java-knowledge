# WebClient & WebFlux Tests

Testing reactive code requires specialized testing tools: standard imperative assertions fail because reactive streams execute asynchronously across event loops and schedulers.

## Testing Strategy

1. **Deterministic Stream Assertions with `StepVerifier`**:
   `StepVerifier` from `reactor-test` acts as a subscriber that controls and verifies signals emitted by a `Mono` or `Flux`:
   - Verifies item emission values and order (`expectNext`, `assertNext`).
   - Asserts terminal completion (`expectComplete`, `verifyComplete`).
   - Verifies expected errors (`expectError`, `expectErrorMatches`).
2. **Virtual Time Testing (`StepVerifier.withVirtualTime`)**:
   Allows testing delayed operators (`Mono.delay()`, `retryWhen(Retry.backoff(...))`) in milliseconds without executing actual wall-clock thread sleeps.
3. **HTTP Wire Contract Verification with WireMock**:
   Uses `WireMockServer` to simulate downstream services, verifying non-blocking execution, channel timeouts, retries, and partial error degradation.

## Test Suite Overview

```bash
# Fast unit tests (in-process, no Docker)
./gradlew :modules:21-webclient-webflux:test

# WireMock end-to-end integration tests
./gradlew :modules:21-webclient-webflux:integrationTest
```

| Test Class | Invariant Proved | Technique |
|---|---|---|
| `CorrectReactivePaymentGatewayTest` | Non-blocking execution returns REJECTED without stalling on invalid tokens | `StepVerifier` + mock WebClient |
| `CorrectCustomerSummaryServiceTest` | Offloaded JDBC queries execute on `boundedElastic` and propagate errors | Mockito `JdbcClient` + `StepVerifier` |
| `CorrectNotificationBatchSenderTest` | Bounded flatMap isolates errors per message and completes full batch | `StepVerifier` assertion on each element |
| `CorrectCarrierTrackingClientTest` | Client times out gracefully and returns fallback tracking status | `StepVerifier` + timeout verification |
| `CorrectCartPricingEngineTest` | Multi-source aggregator degrades to 0 discounts on downstream service failures | `StepVerifier` assertion on tuple results |
| `WebFluxIntegrationTest` | WireMock integration testing non-blocking HTTP requests, timeouts, and partial 500s | WireMock + `StepVerifier` |

## Key Test Snippets

### Proving Non-Blocking Error Isolation in Batch Processing

```java
--8<-- "modules/21-webclient-webflux/src/test/java/lab/webflux/flatmapconcurrency/CorrectNotificationBatchSenderTest.java"
```

### Proving JDBC Offloading to `boundedElastic`

```java
--8<-- "modules/21-webclient-webflux/src/test/java/lab/webflux/blockingjdbc/CorrectCustomerSummaryServiceTest.java"
```

### Proving WireMock End-to-End Non-Blocking Integration

```java
--8<-- "modules/21-webclient-webflux/src/integrationTest/java/lab/webflux/WebFluxIntegrationTest.java"
```

## Related

- [Concepts](concepts.md) — Reactive Streams subscription and execution model
- [Code Review](code-review.md) — Real-world anti-patterns in reactive Spring pipelines
- [Solutions](solutions.md) — Production-grade non-blocking implementations
- [Production](production.md) — Operational diagnostics and monitoring
