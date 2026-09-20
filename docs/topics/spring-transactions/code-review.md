# Spring Transactions Code Review

Review each clean source before expanding its answer.

## Self-invocation transaction bypass

A developer created an order processing service where `processOrder` invokes `placeOrder` annotated with `@Transactional`.

```java
--8<-- "modules/07-spring-transactions/broken-examples/self-invocation/OrderService.java"
```

Consider Spring AOP dynamic proxy mechanics, method invocation on `this`, and transactional interceptor boundaries.

??? warning "Reveal issues"
    **Architecture issue — self-invocation bypasses Spring AOP transactional proxy:** `processOrder()` calls `placeOrder()` directly on `this`. In standard Spring AOP proxies, method execution does not cross the proxy boundary, so `TransactionInterceptor` is never invoked and no database transaction is started.

[Correct implementation](solutions.md#proxy-safe-transactional-delegation)

---

## Remote API call inside transaction

A checkout service executes a remote payment HTTP call inside a `@Transactional` boundary, leading to connection pool exhaustion during flash sales.

```java
--8<-- "modules/07-spring-transactions/broken-examples/remote-api-in-transaction/PaymentClient.java"
```

```java
--8<-- "modules/07-spring-transactions/broken-examples/remote-api-in-transaction/CheckoutService.java"
```

Consider database connection lifecycles, HikariCP pool capacity, and network I/O latency inside database transactions.

??? warning "Reveal issues"
    **Performance issue — remote HTTP call inside @Transactional holds DB connection:** `CheckoutService.checkout()` acquires a database connection from HikariCP upon entering the method and holds it idle while waiting for the multi-second remote payment HTTP call to return, exhausting the connection pool under load.

    **Architecture fix — narrowed transaction boundaries:** Extract remote HTTP network calls outside `@Transactional` methods into a non-transactional orchestrator.

[Correct implementation](solutions.md#isolated-transaction-boundaries)

---

## Checked exception rollback assumption

An order placement service throws a checked exception upon validation failure, assuming Spring will automatically roll back the transaction.

```java
--8<-- "modules/07-spring-transactions/broken-examples/checked-exception-rollback/OrderValidationException.java"
```

```java
--8<-- "modules/07-spring-transactions/broken-examples/checked-exception-rollback/OrderPlacementService.java"
```

Consider Spring default rollback policies for checked versus unchecked exceptions.

??? warning "Reveal issues"
    **Reliability issue — checked exception commits transaction by default:** By default, Spring rolls back transactions only on `RuntimeException` and `Error`. Throwing a checked `Exception` results in the transaction being committed.

    **Reliability fix — explicit rollback rule:** Declare `@Transactional(rollbackFor = {OrderValidationException.class, Exception.class})` or throw domain `RuntimeException` types.

[Correct implementation](solutions.md#explicit-rollback-rules)

---

## Wrong propagation and connection pool exhaustion

An audit service configured with `Propagation.REQUIRES_NEW` is called inside an active transaction, causing pool deadlocks under concurrent traffic.

```java
--8<-- "modules/07-spring-transactions/broken-examples/wrong-propagation-requires-new/AuditLogService.java"
```

```java
--8<-- "modules/07-spring-transactions/broken-examples/wrong-propagation-requires-new/OrderService.java"
```

Consider transaction propagation mechanics, nested connection acquisition, and HikariCP pool sizing limits.

??? warning "Reveal issues"
    **Reliability issue — REQUIRES_NEW inside active transaction causes connection pool deadlock:** When `OrderService` calls `AuditLogService.recordAudit()` with `Propagation.REQUIRES_NEW`, Spring suspends the outer transaction (holding connection $C_1$) and attempts to acquire a second connection $C_2$ from HikariCP on the same thread. If all pool connections are held by suspended outer transactions, the application enters an unrecoverable connection pool deadlock.

[Correct implementation](solutions.md#single-connection-propagation)

---

## Asynchronous execution across transaction boundaries

An order service invokes an `@Async` notification method from within an active transaction, causing phantom notifications when transactions roll back.

```java
--8<-- "modules/07-spring-transactions/broken-examples/async-transaction-boundary/NotificationService.java"
```

```java
--8<-- "modules/07-spring-transactions/broken-examples/async-transaction-boundary/OrderService.java"
```

Consider `ThreadLocal` transaction context isolation and asynchronous method dispatch timing.

??? warning "Reveal issues"
    **Reliability issue — async task dispatched before transaction commit:** `@Async` runs on a separate worker thread that does not inherit the calling `ThreadLocal` transaction. If the transaction rolls back after dispatching the async task, the notification is sent for an order that never committed.

    **Reliability fix — after-commit transactional event listener:** Publish domain events within the transaction and handle them using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.

[Correct implementation](solutions.md#after-commit-transactional-events)

---

## Transaction on private method

An inventory service marks an internal `private` helper method with `@Transactional`, expecting atomic updates.

```java
--8<-- "modules/07-spring-transactions/broken-examples/transaction-on-private-method/InventoryService.java"
```

Consider Spring AOP proxy interception capabilities and access modifier visibility rules.

??? warning "Reveal issues"
    **Architecture issue — @Transactional on private method is silently ignored:** Spring AOP dynamic proxies (CGLIB subclassing and JDK dynamic proxies) can only override and intercept `public` methods. Placing `@Transactional` on `private` methods is silently ignored by Spring without warning.

[Correct implementation](solutions.md#public-collaborator-transaction-boundary)

---

## Comprehensive order processing workflow PR

A combined pull request containing multiple subtle transaction defects across self-invocation, remote API calls, checked exception rollback, and notification timing.

```java
--8<-- "modules/07-spring-transactions/broken-examples/order-processing-v1/OrderProcessingWorkflow.java"
```

Consider end-to-end transactional orchestration, short transaction lifecycles, and resilient event publication.

??? warning "Reveal issues"
    **Architecture issue — self-invocation bypass:** `processOrder` calls `executeTransaction` on `this`, bypassing proxy interceptors.

    **Performance issue — remote payment call in transaction:** `chargePaymentGateway` blocks database connections.

    **Reliability issue — missing rollbackFor on checked exception:** Throwing `java.lang.Exception` does not trigger rollback.

    **Reliability issue — premature notification dispatch:** `sendCustomerEmail` runs without waiting for database transaction commit.

[Correct implementation](solutions.md#resilient-order-workflow-coordinator)
