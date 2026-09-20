# Spring Transactions Solutions

Production-grade implementations corresponding to the code review exercises.

## Proxy-safe transactional delegation

### Implementation

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/selfinvocation/OrderPlacementCollaborator.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/selfinvocation/OrderService.java"
```

### Why it works

1. **Collaborator Bean Boundary**: Moving the transactional operation to `OrderPlacementCollaborator` ensures calls from `OrderService` cross the Spring AOP proxy boundary.
2. **Proxy Interception Active**: `TransactionInterceptor` properly opens and closes physical transactions around `placeOrder()`.

### Trade-offs

Introducing collaborator beans increases the total number of classes, but preserves declarative transaction boundaries cleanly without resorting to self-injected proxies or programmatic transaction managers.

---

## Isolated transaction boundaries

### Implementation

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/externalcall/OrderRecord.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/externalcall/OrderRepository.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/externalcall/PaymentClient.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/externalcall/CheckoutOrchestrator.java"
```

### Why it works

1. **Non-Transactional Orchestrator**: `CheckoutOrchestrator` coordinates the workflow without opening a wide database transaction.
2. **Short Atomic Database Transactions**: `savePendingOrder` and `updateStatus` execute in independent short transactions lasting only a few milliseconds.
3. **Zero Connection Holding on Network I/O**: The remote payment HTTP call executes when no database connection is held.

### Trade-offs

Requires explicit state tracking (e.g. `PENDING`, `PAID`, `PAYMENT_FAILED`) and potential compensating transactions or background reconciliation if the system crashes midway.

---

## Explicit rollback rules

### Implementation

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/rollbackrules/OrderValidationException.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/rollbackrules/OrderPlacementService.java"
```

### Why it works

1. **`rollbackFor` Attribute**: `@Transactional(rollbackFor = {OrderValidationException.class, Exception.class})` explicitly instructs Spring's `RuleBasedTransactionAttribute` to roll back when checked exceptions are thrown.
2. **Consistent Atomicity**: Pre-existing database modifications are reverted cleanly when validation errors occur.

### Trade-offs

Requires developers to maintain explicit `rollbackFor` lists across all transactional methods throwing checked exceptions, or migrate domain exception hierarchies to unchecked `RuntimeException`s.

---

## Single connection propagation

### Implementation

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/propagation/AuditLogService.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/propagation/OrderService.java"
```

### Why it works

1. **Single Connection Participation**: Using `Propagation.REQUIRED` allows `AuditLogService` to reuse the existing database connection and transaction context from `OrderService`.
2. **Eliminates Pool Deadlock**: Prevents holding multiple connections concurrently on the same thread.

### Trade-offs

If audit logging fails, the entire transaction will roll back. If audit logging must persist independently, asynchronous out-of-band audit logging via message queues (e.g. Kafka) or transactional outbox tables is preferred over synchronous `REQUIRES_NEW`.

---

## After-commit transactional events

### Implementation

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/asynctransaction/OrderCreatedEvent.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/asynctransaction/OrderNotificationListener.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/asynctransaction/OrderService.java"
```

### Why it works

1. **Deferred Event Processing**: `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` defers notification handling until the physical database transaction has successfully committed.
2. **Rollback Safety**: If `OrderService.createOrder` throws an exception, the event is automatically discarded, preventing phantom customer emails.

### Trade-offs

Event handling executes after the database connection is committed and closed; any database updates in the event listener require a new transaction (`@Transactional(propagation = Propagation.REQUIRES_NEW)`).

---

## Public collaborator transaction boundary

### Implementation

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/publicboundary/StockReservationCollaborator.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/publicboundary/InventoryService.java"
```

### Why it works

1. **Public Method Visibility**: `deductStockTransactional` is declared `public` on a Spring-managed component, allowing CGLIB and JDK dynamic proxies to override and wrap the invocation.
2. **Proxy Interception**: Transaction advice executes reliably on every call.

### Trade-offs

Exposes methods at public package visibility, which can be mitigated by keeping collaborator classes package-private while ensuring methods remain `public` within the package.

---

## Resilient order workflow coordinator

### Implementation

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/orderworkflow/OrderWorkflowCommand.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/orderworkflow/OrderWorkflowResult.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/orderworkflow/OrderCompletedEvent.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/orderworkflow/OrderRepository.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/orderworkflow/PaymentGatewayClient.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/orderworkflow/OrderNotificationListener.java"
```

```java
--8<-- "modules/07-spring-transactions/src/main/java/lab/springtransactions/orderworkflow/OrderProcessingCoordinator.java"
```

### Why it works

1. **Decoupled Architecture**: Separates orchestration (`OrderProcessingCoordinator`), database operations (`OrderRepository`), remote APIs (`PaymentGatewayClient`), and event subscribers (`OrderNotificationListener`).
2. **Minimal Transaction Footprint**: Short atomic database operations prevent HikariCP connection starvation.
3. **Rollback and Notification Safety**: External side effects trigger strictly upon successful database state transitions.

### Trade-offs

Requires designing explicit workflow states and domain events, but provides enterprise-grade reliability and scalability under high concurrency.
