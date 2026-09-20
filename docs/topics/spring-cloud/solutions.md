# Spring Cloud Solutions

Walkthrough of correct implementations addressing the edge routing, declarative RPC, and configuration anti-patterns identified during code review.

---

## Edge Gateway Timeouts, Resilience, and Rate Limiting

The corrected Spring Cloud Gateway configuration establishes strict global Netty HTTP client timeouts, connection pool sizing, internal header sanitization, Redis token-bucket rate limiting, and Resilience4j circuit breaker integration with fallback routes.

### Corrected Configuration

```yaml
--8<-- "modules/20-spring-cloud/broken-examples/gateway-unbounded-routing/correct/application.yml"
```

### Why It Works

1. **Explicit Multi-Tier Timeouts**:
   - `spring.cloud.gateway.httpclient.connect-timeout: 1000` bounds TCP connection establishment to 1 second.
   - `spring.cloud.gateway.httpclient.response-timeout: 3s` ensures that any backend call taking longer than 3 seconds is aborted, freeing Netty socket buffers.
   - Per-route metadata (`metadata.response-timeout: 2000`) allows fine-tuning timeouts for specific services (e.g. fast catalog queries vs slower batch endpoints).
2. **Elastic Connection Pool**:
   - Setting `pool.type: ELASTIC` and `pool.max-connections: 500` bounds the total number of outbound backend TCP connections while pruning idle sockets after 30 seconds (`max-idle-time: 30s`).
3. **Defense-in-Depth Security**:
   - Global `default-filters` enforce `RemoveRequestHeader=X-Internal-Secret` and `RemoveRequestHeader=X-User-Id`. Any spoofed internal authentication headers submitted by external clients are stripped before routing. Verified internal tokens are injected via environment variable references (`${INTERNAL_GATEWAY_SECRET}`).
4. **Token Bucket Rate Limiting**:
   - Each route is equipped with a `RequestRateLimiter` filter backed by Redis. Requests exceeding the replenish rate and burst capacity receive immediate HTTP `429 Too Many Requests` at the edge before consuming backend compute.
5. **Circuit Breaking and Fallbacks**:
   - The `CircuitBreaker` filter wraps backend routing in a Resilience4j circuit breaker. When downstream failure rates exceed 50%, requests fail fast and forward internally to graceful degradation endpoints (`forward:/fallback/orders`).

### Operational Trade-offs

- **Rate Limiting Redis Dependency**: If the Redis instance backing the rate limiter experiences latency spikes or connection failures, the gateway's `RequestRateLimiter` may fail open or closed depending on configuration (`deny-empty-key`). Redis must be highly available (Redis Sentinel or Cluster).
- **Fallback Semantics**: Fallbacks must return idempotent, degraded information (e.g. empty lists, cached recommendations) and must never execute heavy blocking computations.

---

## Feign Timeouts, Custom ErrorDecoder, and Resilience

The corrected OpenFeign client configures explicit connection and read timeouts via `Request.Options`, disables blind Feign retries, implements an `ErrorDecoder` that maps HTTP statuses to domain exceptions, and attaches a `FallbackFactory` for graceful circuit breaking.

### Corrected Implementation

```java
--8<-- "modules/20-spring-cloud/broken-examples/feign-missing-timeout-error-decoder/correct/InventoryClient.java"
```

### Why It Works

1. **Strict Connection & Read Timeouts**:
   - `Request.Options(500, TimeUnit.MILLISECONDS, 2000, TimeUnit.MILLISECONDS, true)` guarantees that TCP connection hangs abort after 500ms and slow socket reads abort after 2000ms. Calling worker threads are never held indefinitely.
2. **Elimination of Blind Retries**:
   - Returning `Retryer.NEVER_RETRY` prevents Feign from automatically retrying failed requests. Mutating POST operations cannot be accidentally redelivered by low-level HTTP client hooks.
3. **Domain-Specific Error Decoding**:
   - `InventoryErrorDecoder` inspects the HTTP status code:
     - `404` $\to$ `ItemNotFoundException` (business error, clean handling).
     - `409` / `422` $\to$ `InsufficientStockException` (business error, clean handling).
     - `503` / `504` $\to$ `RetryableException` (transient infrastructure error, signal for external Resilience4j retry with backoff).
     - Other statuses delegate to the default decoder.
4. **Safe Fallback Degradation**:
   - `InventoryFallbackFactory` distinguishes between read operations and write operations:
     - For `getStock(sku)` (read-only): returns a safe degraded DTO with 0 stock.
     - For `reserveStock(sku, ...)` (mutating write): throws `ServiceUnavailableException` immediately, preventing silent corruption of order state when inventory reservation cannot be verified.
5. **Idempotency Enforcement**:
   - The mutating `reserveStock` method explicitly requires an `@RequestHeader("Idempotency-Key")` string, ensuring that any upper-level retries are deduplicated by the downstream inventory service.

### Operational Trade-offs

- **Fallback Risk**: Returning fallbacks on read queries can mask outages from monitoring if metrics do not track circuit breaker trips and fallback executions.
- **Modern Alternative**: While Feign provides clean declarative interfaces, new Spring Boot 3.5+ services should evaluate **Spring 6 HTTP Interfaces** (`@HttpExchange`) with `RestClient`, which natively integrates with Micrometer Observation and avoids third-party proxy dependencies.
