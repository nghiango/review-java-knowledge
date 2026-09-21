# Code Review: Testing Anti-Patterns in Spring Boot 4 & Java 25

!!! info "Delta from baseline"
    Baseline code reviews in [`modules/12-testing`](../../../topics/testing/code-review.md) focus on test assertion smells and mocking overuse.
    This page presents two review targets showcasing concurrency test flakiness and REST assertion drift under modern Spring and Java 25.

---

## Review Target 1: Unbound Concurrency Test Flakiness

### Context
A notification service dispatches async tasks using unbound virtual threads. The accompanying test suite attempts to synchronize using arbitrary `Thread.sleep` calls and asserts against internal thread counts rather than domain outcomes, leading to chronic CI build failures.

```java
--8<-- "tracks/java25-boot4/modules/12-testing/broken-examples/unbound-concurrency-test-flakiness/OrderNotificationService.java"
```

```java
--8<-- "tracks/java25-boot4/modules/12-testing/broken-examples/unbound-concurrency-test-flakiness/AsyncOrderProcessingTest.java"
```

??? tip "Review Guidance"
    - Why does `Thread.sleep(150)` produce flakiness under CPU-throttled CI runners?
    - What is the risk of fire-and-forget virtual thread spawning without structured lifecycle coordination?
    - How does asserting `getLiveThreadCount()` couple the test to implementation details?

---

## Review Target 2: REST Test Client Assertion Drift

### Context
A legacy REST endpoint test suite asserts against raw string bodies and ignores RFC 9457 ProblemDetail standards. When error handling is refactored, the test assertions fail or mask critical regressions in client contracts.

```java
--8<-- "tracks/java25-boot4/modules/12-testing/broken-examples/rest-test-client-assertion-drift/CustomerOrderApiController.java"
```

```java
--8<-- "tracks/java25-boot4/modules/12-testing/broken-examples/rest-test-client-assertion-drift/LegacyOrderEndpointTest.java"
```

??? tip "Review Guidance"
    - How do raw string matchers mask breaking changes in error payloads?
    - Why is RFC 9457 `ProblemDetail` structural validation critical for microservice client SDKs?
    - What are the advantages of fluent JSON path assertions over exact string equality?
