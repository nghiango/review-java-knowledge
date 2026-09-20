# Spring Transactions Tests

## Transaction Testing Strategy

Testing transactional services requires verifying both successful state transitions and rollback behavior under failure:

1. **Pure Unit Testing with Constructor Injection**: Isolates business orchestration, state transitions, and exception rules without needing heavy container startup.
2. **Boundary Testing**: Verifies that remote network calls execute outside database transactions and that database operations remain short and atomic.
3. **Rollback Verification**: Proves that checked exceptions configured with `rollbackFor` and unchecked exceptions revert all state changes.
4. **Event Synchronization Assertions**: Verifies that `@TransactionalEventListener` subscribers only process events on successful transaction commits.

## Test Suite Overview

```bash
./gradlew :modules:07-spring-transactions:test
```

| Test Class | Invariant Proved | Technique |
|---|---|---|
| `SelfInvocationTransactionTest` | Collaborator bean delegates invocations across proxy boundaries | Unit verification |
| `ExternalCallTransactionBoundaryTest` | Network I/O decoupled from short atomic database transactions | Unit state assertions |
| `RollbackRulesTransactionTest` | Explicit `rollbackFor` ensures checked exceptions roll back state | Checked exception assertion |
| `PropagationTransactionTest` | `Propagation.REQUIRED` participates in transaction without extra connections | Audit logging assertions |
| `AsyncTransactionEventListenerTest` | Events published inside transaction are handled safely after commit | Event publisher verification |
| `PublicBoundaryTransactionTest` | Public collaborator methods enforce transactional inventory reservation | Unit inventory verification |
| `OrderWorkflowTest` | End-to-end order orchestration: pending state, payment, completion, event | Mockito and state assertions |

## Key Test Snippets

### Proving Decoupled Transaction Boundaries on External Calls

```java
--8<-- "modules/07-spring-transactions/src/test/java/lab/springtransactions/externalcall/ExternalCallTransactionBoundaryTest.java"
```

### Proving Explicit Checked Exception Rollback

```java
--8<-- "modules/07-spring-transactions/src/test/java/lab/springtransactions/rollbackrules/RollbackRulesTransactionTest.java"
```

### Proving After-Commit Transactional Event Publishing

```java
--8<-- "modules/07-spring-transactions/src/test/java/lab/springtransactions/asynctransaction/AsyncTransactionEventListenerTest.java"
```

### Proving Resilient Order Processing Workflow

```java
--8<-- "modules/07-spring-transactions/src/test/java/lab/springtransactions/orderworkflow/OrderWorkflowTest.java"
```

## Related

- [Solutions](solutions.md)
- [Code Review](code-review.md)
- [Production](production.md)
