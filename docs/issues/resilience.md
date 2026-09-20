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

## Related

- [Issue catalogue](index.md)
