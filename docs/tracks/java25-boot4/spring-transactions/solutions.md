# Solutions: Spring Transactions Refactoring in Spring Boot 4

!!! info "Delta from baseline"
    Baseline solutions in [`docs/topics/spring-transactions/solutions.md`](../../../topics/spring-transactions/solutions.md) resolve classical proxy boundaries, rollback rules, and transactional event listeners.
    This page details production solutions for **Spring Boot 4 / Java 25 transaction traps**: orchestrating parallel virtual subtasks outside database transactions and propagating context via `ScopedValue`.

---

## Solution 1: Safe Orchestration of Virtual Subtasks with Database Transactions

--8<-- "tracks/java25-boot4/modules/07-spring-transactions/broken-examples/structured-scope-transaction-leak/SOLUTION.md"

### Production Reference Implementation

Here is the clean implementation separating short database transactions from parallel `StructuredTaskScope` verification:

```java
--8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/main/java/lab/java25boot4/springtransactions/CoordinatedOrderWorkflowService.java"
```

### Key Architectural Benefits
- **Zero Connection Holding During I/O**: Network latency from remote services does not consume database connections.
- **ACID Atomicity**: State changes occur in explicit atomic blocks, avoiding partial commits.

---

## Solution 2: Scoped Value Transaction Context Coordination

--8<-- "tracks/java25-boot4/modules/07-spring-transactions/broken-examples/virtual-thread-threadlocal-transaction-loss/SOLUTION.md"

### Production Reference Implementation

Here is the production implementation using `ScopedValue` to safely propagate transactional correlation metadata to virtual subtasks:

```java
--8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/main/java/lab/java25boot4/springtransactions/ScopedTransactionContextCoordinator.java"
```

### Event Listener Post-Commit Verification

```java
--8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/main/java/lab/java25boot4/springtransactions/TransactionalEventListenerVerification.java"
```
