# Resilience Tests

Testing resilience mechanisms requires verifying both algorithmic behavior (bounds, intervals, thresholds) and fault-injection semantics across simulated network failure conditions.

## Testing Strategy

1. **Fast Unit Testing with Mockito**: Proves retry bounds, exception classification, and circuit breaker state transitions in milliseconds without external processes.
2. **Deterministic Fault Injection with WireMock**: Simulates HTTP transport latency, 503 Service Unavailable errors, and socket read timeouts to verify client timeouts and idempotency key attachment.
3. **State Machine Verification**: Asserts that Circuit Breakers correctly advance through `CLOSED` $\to$ `OPEN` $\to$ `HALF_OPEN` states and fast-fail subsequent requests when degraded.

## Test Suite Overview

```bash
# Fast unit tests (no Docker required)
./gradlew :modules:18-resilience:test

# HTTP fault-injection integration tests
./gradlew :modules:18-resilience:integrationTest
```

| Test Class | Invariant Proved | Technique |
|---|---|---|
| `SafeOrderPaymentServiceTest` | Bounded retries stop after 3 attempts; fails fast on non-retryable 4xx errors | Mockito unit verification |
| `SafeInventorySyncServiceTest` | Fallback activates gracefully when downstream server connection fails | Mockito / RestClient test |
| `SafeCurrencyRateServiceTest` | Retries with full jitter succeed upon downstream recovery | Mockito unit verification |
| `SafeCustomerRiskServiceTest` | CircuitBreaker trips to OPEN on 50% failure rate; fast-fails subsequent calls | Resilience4j state inspection |
| `ResilienceIntegrationTest` | TimeLimiter fallback triggers on delayed response; Idempotency-Key is attached across retries | WireMock HTTP simulation |

## Key Test Snippets

### Proving Bounded Retries & Non-Retryable Exceptions

```java
--8<-- "modules/18-resilience/src/test/java/lab/resilience/boundedretry/SafeOrderPaymentServiceTest.java"
```

### Proving Circuit Breaker State Transitions & Fast Failure

```java
--8<-- "modules/18-resilience/src/test/java/lab/resilience/circuitbreaker/SafeCustomerRiskServiceTest.java"
```

### Proving TimeLimiter & Idempotency Key Attachment with WireMock

```java
--8<-- "modules/18-resilience/src/integrationTest/java/lab/resilience/ResilienceIntegrationTest.java"
```

## Related

- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Concepts](concepts.md)
- [Internals](internals.md)
- [Production](production.md)
