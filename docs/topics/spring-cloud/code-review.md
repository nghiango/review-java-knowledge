# Spring Cloud Code Review

Review each clean configuration and code snippet before revealing the issues and architectural explanations.

---

## Spring Cloud Gateway unbounded routing

An engineering team authored an edge routing configuration for Spring Cloud Gateway (`application.yml`) to expose internal microservices to external traffic.

```yaml
--8<-- "modules/20-spring-cloud/broken-examples/gateway-unbounded-routing/application.yml"
```

Consider global Netty client connect and response timeout boundaries, CircuitBreaker fallback protection, distributed rate limiting, and internal security header sanitization.

??? warning "Reveal issues"
    **Resilience issue — missing global Netty HTTP client connect and response timeouts:** By default, Reactor Netty's `HttpClient` runs with unbounded response timeouts and high connect timeouts. When a downstream microservice slows down or hangs, pending gateway connections never complete, consuming TCP sockets and Netty event loop memory until the edge gateway suffers total outage.

    **Resilience issue — routes lack CircuitBreaker filter and fallback endpoints:** If a downstream service crashes or degrades, all incoming traffic hits failing backends directly, triggering connection retries and cascading latency through the edge gateway rather than failing fast.

    **Scalability issue — missing rate limiting filter on public edge routes:** Public endpoints accept arbitrary request volumes without `RequestRateLimiter` filters. A traffic spike or botnet can flood the gateway and saturate downstream microservices.

    **Security issue — static secret token hardcoded in route filter without client header stripping:** Hardcoding internal authentication tokens in YAML risks leakage. Furthermore, without `RemoveRequestHeader` filters, external callers can spoof internal authentication and authorization headers.

[Correct implementation](solutions.md#edge-gateway-timeouts-resilience-and-rate-limiting)

---

## OpenFeign missing timeouts and custom error decoder

A microservice communicates with an internal inventory service via Spring Cloud OpenFeign. An engineer authored `InventoryClient.java` and its configuration.

```java
--8<-- "modules/20-spring-cloud/broken-examples/feign-missing-timeout-error-decoder/InventoryClient.java"
```

Consider connection and read timeout configuration, HTTP status code handling (4xx vs 5xx), blind retry hazards on mutating POST requests, and circuit breaker fallbacks.

??? warning "Reveal issues"
    **Resilience issue — missing explicit connect and read timeouts on Feign client:** No `Request.Options` are configured. Downstream latency spikes or TCP half-open states cause calling Tomcat worker threads to block for up to 60+ seconds, rapidly causing thread pool starvation.

    **Reliability issue — missing custom ErrorDecoder to classify 4xx and 5xx responses:** Feign's default `ErrorDecoder` translates all non-2xx responses into generic `FeignException`. 4xx client errors (e.g. 404 Not Found, 409 Conflict) are not translated into domain exceptions and bubble up as internal server errors (500) to callers.

    **Resilience issue — blind retries (10 attempts) on mutating POST requests:** A `Retryer` with 10 attempts is applied across all endpoints, including non-idempotent `POST /reserve`. If a read timeout occurs after inventory was decremented remotely, retrying up to 10 times causes multiple duplicate reservations and inventory corruption.

    **Resilience issue — missing FallbackFactory for circuit breaking and degradation:** `@FeignClient` lacks a `fallback` or `fallbackFactory`. When the downstream service fails or the circuit breaker opens, calls immediately abort with exceptions rather than returning degraded cached stock numbers.

[Correct implementation](solutions.md#feign-timeouts-custom-errordecoder-and-resilience)
