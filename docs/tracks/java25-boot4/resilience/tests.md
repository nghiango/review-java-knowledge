# Testing Resilience Patterns in Spring Boot 4 & Java 25

!!! info "Delta from baseline"
    Baseline resilience testing in [`modules/18-resilience`](../../../topics/resilience/tests.md) covers testing Resilience4j aspects and Mockito stubbing.
    This page demonstrates testing lock-free circuit breaker state transitions, jittered backoff bounds, and concurrency limit shedding using Awaitility and AssertJ.

---

## 1. Resilience Testing Strategy

Testing resilience mechanisms requires deterministic verification of state machines and timing constraints without relying on arbitrary `Thread.sleep` calls:

1. **Retry Mechanics**: Verify that transient exceptions trigger retries up to `maxAttempts`, while non-retryable exceptions immediately abort.
2. **Circuit Breaker Transitions**:
   - Verify transition from `CLOSED` to `OPEN` when consecutive failures exceed threshold.
   - Verify fast-failing with `CircuitBreakerOpenException` when in `OPEN` state.
   - Verify transition from `OPEN` to `HALF_OPEN` and back to `CLOSED` after `openDuration` using `Awaitility.await().ignoreExceptionsInstanceOf(...)`.
3. **Backoff Math Bounds**: Assert that calculated jitter stays within $[0, \min(\text{maxBackoff}, \text{baseBackoff} \times 2^{\text{attempt}-1})]$.

---

## 2. Unit Test Suite: `ModernResilientExecutionEngineTest.java`

```java
--8<-- "tracks/java25-boot4/modules/18-resilience/src/test/java/lab/java25boot4/resilience/ModernResilientExecutionEngineTest.java"
```

---

## 3. Key Testing Insights

- **Ignoring Expected Transient Exceptions in Awaitility**: When waiting for an open circuit breaker to cool down, early polling attempts may throw `CircuitBreakerOpenException`. Calling `.ignoreExceptionsInstanceOf(CircuitBreakerOpenException.class)` allows Awaitility to keep polling cleanly until the state changes to `HALF_OPEN` and the recovery probe succeeds.
- **Testing Full Jitter Without Flakiness**: Rather than testing for specific random numbers, assert upper and lower mathematical bounds: verify that delays are non-negative and never exceed the exponential ceiling.
