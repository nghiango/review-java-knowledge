# Code Review: Spring Transactions Practice in Spring Boot 4

!!! info "Delta from baseline"
    Baseline code reviews in [`docs/topics/spring-transactions/code-review.md`](../../../topics/spring-transactions/code-review.md) focus on self-invocation, checked exceptions, and remote I/O inside transactions.
    This page presents two code review targets focusing on **Spring Boot 4 / Java 25 transaction traps**: spawning structured scopes inside transactions and losing transaction context across virtual worker threads.

---

## Review Target 1: `BatchOrderService.java`

Review the batch processing service below. Can you spot why spawning a `StructuredTaskScope` inside `@Transactional` breaks ACID atomicity?

```java
--8<-- "tracks/java25-boot4/modules/07-spring-transactions/broken-examples/structured-scope-transaction-leak/BatchOrderService.java"
```

### Review Questions
1. When `scope.fork(...)` executes on child virtual threads, do those subtasks share the parent method's database connection and transaction boundary?
2. If `orderIds.contains("fail-batch")` throws an exception, are the records inserted by the child subtasks rolled back?
3. How should batch ingestion and parallel verification be structured to prevent partial writes?

---

## Review Target 2: `PaymentOrchestrator.java`

Review the payment orchestrator below which passes correlation state to an asynchronous virtual thread using `ThreadLocal`.

```java
--8<-- "tracks/java25-boot4/modules/07-spring-transactions/broken-examples/virtual-thread-threadlocal-transaction-loss/PaymentOrchestrator.java"
```

### Review Questions
1. Why does the forked virtual thread observe `null` when reading from `CURRENT_TX_CORRELATION`?
2. What are the memory leak implications of omitting `remove()` on thread-local storage?
3. How does Java 25 `ScopedValue` resolve context propagation across virtual threads safely?
