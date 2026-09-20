# Resilience Code Review

Review each clean source before expanding its answer.

## Infinite retry loop

An e-commerce order service executes payment settlement against an external payment processor gateway. The developer wrote a loop to handle transient network blips and ensure every order gets charged.

```java
--8<-- "modules/18-resilience/broken-examples/infinite-retry/OrderPaymentService.java"
```

Consider loop termination bounds, delay and backoff algorithms, thread pool exhaustion, and error classification.

??? warning "Reveal issues"
    **Resilience issue — Unbounded while(true) loop:** The payment processing loop has no maximum attempt limit or timeout. When the downstream payment gateway experiences an outage, calling worker threads spin forever and never return. In a Tomcat container with 200 worker threads, 200 concurrent payment requests completely exhaust the container thread pool in seconds, causing total cascading failure.

    **Performance issue — Zero-delay tight loop retry:** Catching the exception and retrying immediately with zero delay pins CPU cores at 100% and generates a massive flood of HTTP requests against an already failing downstream server.

    **Reliability issue — Retrying permanent non-retryable exceptions:** Catching generic `Exception` retries permanent 4xx errors (e.g. 400 Bad Request, 402 Card Declined, invalid credentials) in a futile loop instead of failing fast.

    **Observability issue — Standard error logging:** Logging via `System.err.println` bypasses structured logging, MDC tracing, and centralized log aggregation.

[Correct implementation](solutions.md#bounded-retries-with-exponential-backoff)

---

## Retry without timeout

An internal warehouse inventory service is polled by the order fulfillment pipeline to verify physical item availability before reservation. The developer implemented a bounded retry loop with linear backoff.

```java
--8<-- "modules/18-resilience/broken-examples/retry-without-timeout/InventorySyncService.java"
```

Consider default connect and socket read timeouts on `RestClient`, thread blockage behavior during downstream hangs, and backoff mechanics.

??? warning "Reveal issues"
    **Resilience issue — Infinite socket read timeout:** `RestClient` built without an explicitly configured `ClientHttpRequestFactory` defaults to infinite read timeout on standard JDK `HttpURLConnection`. If the warehouse server accepts TCP connections but hangs while generating the response, the calling thread blocks forever.

    **Configuration issue — Missing connect timeout:** Omission of a connect timeout allows threads to hang for minutes waiting for operating system TCP SYN retransmissions during network black holes.

    **Reliability issue — Latency amplification across retries:** Retrying operations without a strict timeout hierarchy multiplies thread lockup duration: 3 attempts with unbounded timeouts locks threads for an indefinite period, depleting container thread pools.

[Correct implementation](solutions.md#strict-timeouts-and-timelimiter)

---

## Retrying non-idempotent call

A subscription billing engine charges customer credit cards via an internal billing microservice using `POST /api/v1/charges`. To guard against network drops, the author caught `ResourceAccessException` (which includes socket read timeouts) and added automatic retries.

```java
--8<-- "modules/18-resilience/broken-examples/retrying-non-idempotent-call/BillingService.java"
```

Consider idempotency guarantees on mutating HTTP `POST` endpoints, network timeout ambiguity, and duplicate customer charges.

??? warning "Reveal issues"
    **Data consistency issue — Retrying non-idempotent POST without idempotency keys:** `POST /api/v1/charges` is a mutating operation. When a `ResourceAccessException` occurs due to a socket read timeout, the downstream billing server may have already successfully processed the transaction before the network connection dropped. The subsequent retry executes a second charge, double-billing the customer.

    **Reliability issue — Read timeout ambiguity:** Network timeouts represent an unknown state, not a failure state. Retrying mutating requests requires an `Idempotency-Key` header verified by the receiver.

    **Performance issue — Linear backoff without jitter:** Retrying with fixed linear delay ($500\text{ms} \times \text{attempt}$) risks synchronizing concurrent failed callers.

[Correct implementation](solutions.md#idempotent-retries-with-deduplication-keys)

---

## Retry storm missing jitter

A cross-border payments application converts foreign exchange rates via an external Forex provider. During peak volume, thousands of worker threads query currency rates concurrently. The developer implemented exponential backoff ($500\text{ms} \times 2^{k-1}$) to relieve pressure on failure.

```java
--8<-- "modules/18-resilience/broken-examples/retry-storm-missing-jitter/CurrencyRateService.java"
```

Consider deterministic backoff synchronization, thundering herd dynamics, and traffic amplification during downstream recovery.

??? warning "Reveal issues"
    **Resilience issue — Synchronized retry storm (Missing Jitter):** The exponential backoff interval is strictly deterministic: $500\text{ms} \times 2^{\text{attempt}-1}$. When 1,000 threads fail concurrently during a transient glitch, all 1,000 threads wake up simultaneously at exactly 500ms, then 1,000ms, then 2,000ms. These synchronized tsunami pulses hammer the recovering Forex provider back offline.

    **Performance issue — Excessive retry amplification:** Configuring 5 retries without a circuit breaker amplifies traffic by 600%, overwhelming struggling dependencies.

    **Reliability issue — Missing circuit breaker:** During prolonged provider outages, requests repeatedly attempt network I/O instead of fast-failing immediately.

[Correct implementation](solutions.md#randomized-jitter-strategies)

---

## Catch and ignore failures

A loan underwriting platform checks external credit bureau scores via `CustomerRiskService`. The developer added a Resilience4j `@CircuitBreaker` annotation with a fallback method to protect against downstream credit bureau degradation, but wrapped the internal call in a `try-catch` block returning a default risk score.

```java
--8<-- "modules/18-resilience/broken-examples/catch-and-ignore-failures/CustomerRiskService.java"
```

Consider Spring AOP exception interception, CircuitBreaker sliding window metrics, and fallback method execution triggers.

??? warning "Reveal issues"
    **Resilience issue — Swallowing exception blinds Circuit Breaker:** Resilience4j's `@CircuitBreaker` uses a Spring AOP around advice that checks whether the intercepted method throws an exception. Because the developer caught the exception inside the method body and returned a fallback risk score, the method completed normally from the viewpoint of the aspect! The CircuitBreaker records every downstream outage as a successful call, keeping the failure rate at 0% and permanently preventing the circuit from tripping to `OPEN`.

    **Observability issue — Swallowed telemetry:** Swallowing exceptions hides errors from Micrometer metrics and Prometheus alerting dashboards.

    **Design issue — Dead fallback method:** The configured `defaultRiskScore` method is never invoked because no exception escapes the method boundary to trigger it.

[Correct implementation](solutions.md#circuit-breaker-with-proper-aop-fallbacks)
