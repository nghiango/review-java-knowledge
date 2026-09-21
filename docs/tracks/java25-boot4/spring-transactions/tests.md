# Testing: Transaction Testing in Spring Boot 4

!!! info "Delta from baseline"
    Baseline testing in [`docs/topics/spring-transactions/tests.md`](../../../topics/spring-transactions/tests.md) covers Testcontainers PostgreSQL testing, rollback assertions, and `@Transactional` test methods.
    This page covers **testing strategies in Spring Boot 4 / Java 25**: asserting transactional boundaries across virtual threads and verifying `ScopedValue` context propagation.

---

## 1. Testing Coordinated Transaction Boundaries

Ensure unit tests assert that:
1. When all parallel subtasks succeed, the final entity status is committed.
2. When any parallel subtask throws an exception, compensating updates or failure statuses are recorded.
3. Database connections are not held across asynchronous operations.

### Example Test Suite

```java
--8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/test/java/lab/java25boot4/springtransactions/CoordinatedOrderWorkflowServiceTest.java"
```

---

## 2. Testing `ScopedValue` Context Inheritance

Verify that virtual workers forked within a structured task scope inherit transaction correlation metadata seamlessly:

```java
--8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/test/java/lab/java25boot4/springtransactions/ScopedTransactionContextCoordinatorTest.java"
```

---

## 3. Testing Post-Commit Event Listeners

Assert that event listeners fire and handle domain events safely:

```java
--8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/test/java/lab/java25boot4/springtransactions/TransactionalEventListenerVerificationTest.java"
```
