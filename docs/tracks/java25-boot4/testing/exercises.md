# Exercises: Testing in Spring Boot 4 & Java 25

!!! info "Delta from baseline"
    Baseline exercises in [`modules/12-testing`](../../../topics/testing/exercises.md) cover standard mock refactorings and Testcontainers wiring.
    These exercises practice refactoring flaky asynchronous tests to deterministic Awaitility polling and writing ArchUnit rules.

---

## Exercise 1: Refactor Flaky Thread.sleep to Awaitility

### Problem Description
You are given a legacy test suite that tests an asynchronous order status dispatcher. The test currently uses `Thread.sleep(500)` to wait for event emission:

```java
// Flaky legacy pattern
orderService.dispatchOrderAsync("ord-100");
Thread.sleep(500);
assertThat(orderService.isDispatched("ord-100")).isTrue();
```

### Tasks
1. Remove `Thread.sleep(500)`.
2. Introduce `Awaitility.await()` with an upper bound of 2 seconds and a 10ms poll interval.
3. Verify that the test passes reliably even under high CPU load.

---

## Exercise 2: Enforce Package Boundaries with ArchUnit

### Problem Description
Ensure that internal implementation classes within `lab.java25boot4.testing` are not accessed directly by external client packages.

### Tasks
1. Define an `ArchRule` using `ArchRuleDefinition.noClasses()`.
2. Specify that classes outside `lab.java25boot4.testing` must not access internal classes.
3. Add a check to verify that no production class imports broken-example classes.
