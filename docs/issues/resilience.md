# Resilience Issues

Timeout, retry, circuit-breaker, bulkhead and graceful-degradation failures.

## Entries

### Unbounded Retry Loop Monopolizing Worker Threads

**Type:** Resilience issue · **Severity:** High · **Difficulty:** Basic

**Technology:** Core Java, Spring Boot, Resilience4j · **Interview frequency:** High · **Production impact:** Critical

Executing external remote HTTP calls inside an unbounded `while(true)` loop without attempt limits or backoff delays causes worker threads to spin forever during downstream outages. In standard container thread pools (e.g. Tomcat 200 threads), concurrent requests rapidly deplete all available threads, causing cascading failure across the entire service. Bound retries to a maximum ceiling (e.g. 3 attempts) with exponential backoff and fast fallbacks.

**Appears in:** `modules/18-resilience/broken-examples/infinite-retry`

---

### Missing Socket Read Timeout Pinning Worker Threads

**Type:** Resilience issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Spring RestClient, HTTP Clients · **Interview frequency:** High · **Production impact:** High

Constructing HTTP clients (`RestClient`, `RestTemplate`, or Apache HttpClient) without explicit connect and socket read timeouts defaults to infinite timeout on underlying JDK connections. If a remote server accepts TCP connections but hangs while processing, the calling thread remains stuck in socket read indefinitely. Configure strict multi-layer deadlines: connect timeout ($\le 1\text{s}$) and read timeout ($\le 3\text{s}$).

**Appears in:** `modules/18-resilience/broken-examples/retry-without-timeout`

---

### Synchronized Retry Storm (Thundering Herd) Missing Jitter

**Type:** Resilience issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Distributed Systems, Resilience4j · **Interview frequency:** High · **Production impact:** High

Retrying failed remote calls using deterministic exponential backoff ($500\text{ms}, 1000\text{ms}, 2000\text{ms}$) synchronizes thousands of concurrent failed threads into lockstep waves. When the struggling downstream service attempts to restart, synchronized retry pulses hammer it back offline. Apply Full Jitter (`IntervalFunction.ofExponentialRandomBackoff`) to disperse retry traffic uniformly across the backoff horizon.

**Appears in:** `modules/18-resilience/broken-examples/retry-storm-missing-jitter`

---

### Swallowing Exceptions Blinding Circuit Breaker State Machine

**Type:** Resilience issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Resilience4j, Spring AOP · **Interview frequency:** High · **Production impact:** High

Wrapping remote calls in internal `try-catch` blocks that swallow exceptions inside `@CircuitBreaker` annotated methods prevents the Spring AOP aspect from detecting failures. The CircuitBreaker records every downstream error as a successful invocation, maintaining a 0% failure rate and permanently preventing the circuit from tripping to `OPEN` state. Allow exceptions to escape the method boundary so the CircuitBreaker can record errors and trigger configured fallbacks.

**Appears in:** `modules/18-resilience/broken-examples/catch-and-ignore-failures`

---

### Unbounded Netty HTTP Client Timeouts in Edge Gateway

**Type:** Resilience issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Spring Cloud Gateway, Reactor Netty · **Interview frequency:** High · **Production impact:** Critical

Spring Cloud Gateway routes relying on default Netty `HttpClient` settings operate with unbounded response timeouts and long connect timeouts. When a downstream microservice experiences high latency or hangs, pending gateway connections never complete, consuming outbound TCP sockets and event loop memory until the edge gateway suffers a total cascading outage. Configure global `httpclient.connect-timeout` ($\le 1000\text{ms}$) and `httpclient.response-timeout` ($\le 5\text{s}$) with route-level metadata overrides.

**Appears in:** [Spring Cloud — Gateway unbounded routing](../topics/spring-cloud/code-review.md#spring-cloud-gateway-unbounded-routing)

---

### Missing Declarative Client Timeouts and Blind Feign Retries

**Type:** Resilience issue · **Severity:** High · **Difficulty:** Basic

**Technology:** OpenFeign, Spring Cloud · **Interview frequency:** High · **Production impact:** Critical

Configuring OpenFeign clients without explicit `Request.Options` timeouts allows slow socket reads to hold Tomcat worker threads for up to 60+ seconds. Furthermore, applying Feign's default `Retryer` indiscriminately across mutating POST endpoints generates retry storms and causes duplicate downstream mutations upon socket read timeouts. Enforce explicit multi-layer deadlines (connect $\le 500\text{ms}$, read $\le 2000\text{ms}$) and disable Feign-level retries (`Retryer.NEVER_RETRY`).

**Appears in:** [Spring Cloud — Feign missing timeouts](../topics/spring-cloud/code-review.md#openfeign-missing-timeouts-and-custom-error-decoder)

---

### Missing Inner Stream Error Isolation Cancelling Entire Reactive Batch

**Type:** Resilience issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Project Reactor, Spring WebFlux · **Interview frequency:** High · **Production impact:** High

In Project Reactor, when an inner publisher inside `flatMap` emits an `onError` signal, the outer `Flux` terminates and cancels all other in-flight child subscriptions by default. If a batch process dispatches 1,000 notifications and item #3 encounters a network error, items #4 through #1,000 are immediately aborted. To isolate failures, wrap the inner publisher with `.onErrorResume()` or `.onErrorReturn()` before flattening.

**Appears in:** `modules/21-webclient-webflux/broken-examples/uncontrolled-flatmap-concurrency`

---

### Missing Netty Channel Connect and Response Timeouts on WebClient

**Type:** Resilience issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** WebClient, Reactor Netty · **Interview frequency:** High · **Production impact:** Critical

A default `WebClient` instance created via `WebClient.builder()` does not configure explicit TCP connection or HTTP response timeouts on the underlying Reactor Netty `HttpClient`. If a remote downstream server leaves TCP connections half-open or stalls on transmitting body bytes, Netty connection pool channels remain leased indefinitely, exhausting available sockets and leading to `PoolAcquireTimeoutException`. Always configure `HttpClient.create().responseTimeout(...)` and channel connect timeouts.

**Appears in:** `modules/21-webclient-webflux/broken-examples/missing-webclient-timeout`

---

### Missing Stream-Level Timeout Operator on Reactive WebClient Calls

**Type:** Resilience issue · **Severity:** High · **Difficulty:** Basic

**Technology:** WebClient, Project Reactor · **Interview frequency:** High · **Production impact:** High

While transport-level timeouts guard TCP sockets, reactive pipelines requiring business SLA guarantees must enforce stream-level deadlines. Invoking `webClient.get().retrieve().bodyToMono(...)` without an explicit `.timeout(Duration)` operator leaves callers vulnerable to indefinite delays during slow streaming, TLS handshakes, or proxy buffering. Always chain `.timeout()` with fallback degradation via `.onErrorResume()`.

**Appears in:** `modules/21-webclient-webflux/broken-examples/missing-webclient-timeout`

---

### Terminal onError in Reactive Zip Operators Aborting Multi-Service Composition

**Type:** Resilience issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Spring WebFlux, Project Reactor (`Mono.zip`) · **Interview frequency:** High · **Production impact:** High

In Reactive Streams, the `onError` signal is terminal. When using `Mono.zip()` to execute parallel calls to multiple microservices (e.g. cart price, loyalty discounts, promo coupons), any unhandled error from an optional service (such as coupon recommendation failure) causes `Mono.zip` to immediately abort the entire composite stream and emit an HTTP 500 error, crashing the user's primary checkout journey. Apply `.onErrorReturn()` or `.onErrorResume()` to non-critical upstream dependencies.

**Appears in:** `modules/21-webclient-webflux/broken-examples/chain-without-error-handling`

---

### Fail-Closed Distributed Rate Limiting Collapses Entire Platform During Cache Outage

**Type:** Resilience issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Redis, System Design, Resilience · **Interview frequency:** High · **Production impact:** Critical

Configuring distributed edge or API gateway rate limiters to fail closed (rejecting all client requests with HTTP 429 when Redis cluster connection times out) elevates a caching or auxiliary tier degradation into a catastrophic total platform blackout. Rate limiters should fail open with graceful degradation, logging warnings and falling back to local JVM-bound in-memory limits while allowing production traffic to proceed.

**Appears in:** `modules/27-system-design/broken-examples/distributed-rate-limiter-naive-redis`

## Related

- [Issue catalogue](index.md)
