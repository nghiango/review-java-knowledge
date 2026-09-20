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

## Related

- [Issue catalogue](index.md)
