# Code Review: Concurrency Practice in Java 25

!!! info "Delta from baseline"
    Baseline code reviews in [`docs/topics/concurrency/code-review.md`](../../../topics/concurrency/code-review.md) focus on race conditions, double-checked locking, and thread pool deadlocks.
    This page presents two code review targets focusing on **Java 25 Concurrency traps**: ThreadLocal memory retention on virtual threads and orphan tasks from unstructured concurrency.

---

## Review Target 1: `TenantContextHolder.java`

Review the code below. Can you identify why using `InheritableThreadLocal` in a virtual-thread-dominated architecture causes severe heap degradation and context bleeding?

```java
--8<-- "tracks/java25-boot4/modules/03-concurrency/broken-examples/threadlocal-leak-virtual-threads/TenantContextHolder.java"
```

### Review Questions
1. How does `InheritableThreadLocal` affect heap allocation when 100,000 virtual threads are created per second?
2. What happens to the tenant context if an uncaught exception is thrown during request execution?
3. How does replacing this with `ScopedValue` eliminate both the heap allocation and the cleanup hazard?

---

## Review Target 2: `OrderFulfillmentService.java`

Review the order aggregation service below. Identify what happens when a dependent subtask fails while sibling asynchronous operations are in flight.

```java
--8<-- "tracks/java25-boot4/modules/03-concurrency/broken-examples/unstructured-concurrency-orphan-tasks/OrderFulfillmentService.java"
```

### Review Questions
1. If `reserveInventory` throws `IllegalStateException("Out of stock")`, does `paymentFuture` abort immediately?
2. What happens to database connections or third-party payment gateway calls initiated by `paymentFuture`?
3. How does refactoring to `StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())` enforce failure propagation?
