# Testing Concepts in Spring Boot 4 & Java 25

!!! info "Delta from baseline"
    Baseline module [`modules/12-testing`](../../../topics/testing/concepts.md) covers classical test slice boundaries, mocking strategies, and containerized fixtures.
    This page covers modern testing patterns introduced by Spring Boot 4.0, Spring Framework 7, and Java 25 concurrency primitives.

---

## 1. Unified REST Endpoint Verification

Historically, Spring test suites suffered from architectural fragmentation:
- **`MockMvc`**: Designed exclusively for Spring MVC dispatching without a live network socket.
- **`WebTestClient`**: Introduced for WebFlux reactive pipelines, often repurposed to test MVC endpoints with an artificial client wrapper.
- **`TestRestTemplate`**: Real HTTP socket client lacking modern fluent assertion APIs.

Spring Boot 4 / Framework 7 unifies endpoint testing patterns by enabling fluent, declarative assertions across both simulated dispatch and real network servers. Tests assert:
1. HTTP status codes and headers (e.g., `Content-Type: application/problem+json`).
2. Schema conformity of response bodies.
3. RFC 9457 `ProblemDetail` structures (`type`, `title`, `status`, `detail`, `instance`) and custom extension properties.

---

## 2. Deterministic Virtual Thread Testing

With Java 21+ and Java 25 virtual threads, testing asynchronous workflows requires re-evaluating traditional testing assumptions:
- **`Thread.sleep` anti-pattern**: Using arbitrary `Thread.sleep(500)` in tests creates non-deterministic flakiness in CI pipelines when CPU load spikes, while simultaneously inflating overall build times.
- **Awaitility synchronization**: Tests must use active polling with bounded timeouts (`await().atMost(...).until(...)`) to check condition predicates immediately upon state transition.
- **Structured Concurrency verification**: Workflows executed within `StructuredTaskScope` guarantee that child virtual threads are bounded by lexical scope. Tests can safely verify subtask termination, exception propagation (`FailedException`), and short-circuit cancellation.

---

## 3. RFC 9457 ProblemDetail Assertions

In production microservices, client SDKs rely strictly on RFC 9457 JSON structures for error recovery. Modern test suites verify error contracts explicitly:

```java
mockMvc.perform(get("/api/v2/orders/unknown"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Order Not Found"))
        .andExpect(jsonPath("$.type").value("https://api.example.com/errors/order-not-found"));
```

By testing the error structure directly, engineers prevent client-facing regressions where an unhandled internal exception leaks raw stack traces or breaks JSON schema expectations.

---

## 4. ArchUnit Rules for Java 25 & JSpecify Invariants

Architectural fitness tests prevent architectural erosion before code enters pull request reviews:
- **Package and Layer boundaries**: Enforce that production modules do not accidentally import test harness or review-target packages.
- **JSpecify Nullness Contracts**: Audit that all packages in production source trees declare `@NullMarked` in `package-info.java`.
- **Virtual Thread Safety**: Ensure no blocking synchronized blocks wrap long-running I/O operations that could pin carrier threads.
