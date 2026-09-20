# Resilience Solutions

Production-grade implementations corresponding to the code review exercises.

## Bounded retries with exponential backoff

### Implementation

```java
--8<-- "modules/18-resilience/src/main/java/lab/resilience/boundedretry/OrderPaymentClient.java"
```

```java
--8<-- "modules/18-resilience/src/main/java/lab/resilience/boundedretry/SafeOrderPaymentService.java"
```

### Why it works

1. **Strict Upper Bound**: Caps retry attempts at 3. Under a downstream outage, calls fail quickly after 3 attempts, releasing caller threads.
2. **Exponential Backoff with Full Jitter**: Uses `IntervalFunction.ofExponentialRandomBackoff(50ms, 2.0, 0.5)` to randomize retry delays and prevent synchronized thundering herd spikes.
3. **Exception Filtering**: Retries only `TransientGatewayException` (HTTP 503/504) while immediately failing fast on `InvalidPaymentException` (HTTP 400/402).

### Trade-offs

Slightly increases tail latency on transient failures; failed calls must be propagated to callers or compensation queues.

---

## Strict timeouts and TimeLimiter

### Implementation

```java
--8<-- "modules/18-resilience/src/main/java/lab/resilience/timeout/SafeInventorySyncService.java"
```

### Why it works

1. **Explicit Socket Timeouts**: Configures `SimpleClientHttpRequestFactory` with a 1-second connect timeout and 2-second socket read timeout.
2. **Execution TimeLimiter**: Wraps asynchronous retrieval in Resilience4j `TimeLimiter` with a 2-second deadline and task cancellation (`cancelRunningFuture(true)`).
3. **Graceful Fallback**: Returns a safe `UNAVAILABLE` status record instead of hanging caller threads indefinitely.

### Trade-offs

Legitimate slow database queries on the downstream inventory system will be interrupted if the 2-second deadline is exceeded.

---

## Idempotent retries with deduplication keys

### Implementation

```java
--8<-- "modules/18-resilience/src/main/java/lab/resilience/idempotentretry/ChargeRequest.java"
```

```java
--8<-- "modules/18-resilience/src/main/java/lab/resilience/idempotentretry/SafeBillingService.java"
```

### Why it works

1. **Client-Generated Idempotency Key**: Generates a unique UUID `Idempotency-Key` header per business transaction before initiating retries.
2. **Deduplication Safety Across Retries**: Even if a socket read timeout disconnects before the HTTP 200 response arrives, the subsequent retry passes the exact same `Idempotency-Key`. The downstream billing gateway recognizes the key and returns the cached confirmation without double-charging.
3. **Restricted Retry Scope**: Retries only on network transport failures (`ResourceAccessException`).

### Trade-offs

Downstream microservices must implement an idempotency store (e.g. Redis key or SQL table with unique constraints) to track processed keys.

---

## Randomized jitter strategies

### Implementation

```java
--8<-- "modules/18-resilience/src/main/java/lab/resilience/jitter/SafeCurrencyRateService.java"
```

### Why it works

1. **Full Jitter Distribution**: Leverages `IntervalFunction.ofExponentialRandomBackoff` with a randomization factor of 0.6 to spread retries uniformly across the backoff interval.
2. **De-synchronization of Callers**: Completely eliminates lockstep synchronization when thousands of concurrent currency conversions fail at the same moment.
3. **Controlled Retry Budget**: Limits retries to 4 attempts with exponential growth, ensuring worker threads do not linger indefinitely.

### Trade-offs

Individual retry intervals are non-deterministic, making individual request latencies vary within the jitter boundary.

---

## Circuit Breaker with proper AOP fallbacks

### Implementation

```java
--8<-- "modules/18-resilience/src/main/java/lab/resilience/circuitbreaker/CustomerRiskProfile.java"
```

```java
--8<-- "modules/18-resilience/src/main/java/lab/resilience/circuitbreaker/SafeCustomerRiskService.java"
```

### Why it works

1. **Unswallowed Exception Flow**: Lets exceptions escape to the Resilience4j `CircuitBreaker` decorator, ensuring every downstream failure is registered in the sliding window.
2. **Automatic Fast-Failure**: When failure rate breaches 50% across the 10-call sliding window, the circuit trips to `OPEN`. Subsequent calls immediately throw `CallNotPermittedException` without touching the network.
3. **Typed Fallbacks**: Catches `CallNotPermittedException` to return an explicit fallback risk profile while downstream systems recover.

### Trade-offs

Requires callers to tolerate degraded or default risk assessments while the circuit remains in the `OPEN` state.
