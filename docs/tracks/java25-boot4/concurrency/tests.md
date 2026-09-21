# Testing: Concurrency Testing in Java 25

!!! info "Delta from baseline"
    Baseline testing in [`docs/topics/concurrency/tests.md`](../../../topics/concurrency/tests.md) covers JCStress, Awaitility, and testing thread-safe data structures.
    This page covers **testing strategies for Java 25 Concurrency features**: asserting structured task cancellation, testing scoped values, and validating virtual thread unmounting.

---

## 1. Testing `StructuredTaskScope` Fail-Fast Semantics

When testing structured task scopes, ensure that when any subtask throws an exception:
1. Sibling subtasks receive thread interruption.
2. The scope's `join()` terminates immediately and throws `FailedException`.
3. The root cause is preserved and can be asserted.

### Example Unit Test

```java
--8<-- "tracks/java25-boot4/modules/03-concurrency/src/test/java/lab/java25boot4/concurrency/StructuredConcurrencyOrderAggregatorTest.java"
```

---

## 2. Testing `ScopedValue` Isolation & Rebinding

Scoped values must be verified for:
1. Availability inside the dynamic scope.
2. Unbound state outside the scope (`isBound() == false`).
3. Proper shadowing and restoration in nested scopes.

### Example Unit Test

```java
--8<-- "tracks/java25-boot4/modules/03-concurrency/src/test/java/lab/java25boot4/concurrency/ScopedValueSecurityContextTest.java"
```

---

## 3. Testing ObjectMonitor Virtual Thread Unpinning

To ensure code does not pin carrier threads in production:
1. Use `-Djdk.tracePinnedThreads=full` during JVM test execution.
2. Verify that long-running operations inside `synchronized` blocks yield carrier threads cleanly.

```java
--8<-- "tracks/java25-boot4/modules/03-concurrency/src/test/java/lab/java25boot4/concurrency/VirtualThreadPinningVerificationTest.java"
```
