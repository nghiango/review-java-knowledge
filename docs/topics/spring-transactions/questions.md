# Spring Transactions Interview Questions

Four levels of interview questions covering `@Transactional` proxy mechanics, propagation behaviors, rollback rules, isolation anomalies, HikariCP connection pools, and production incident remediation.

<!-- --8<-- [start:basic] -->
## Basic

### 1. How does Spring's `@Transactional` annotation work under the hood and why does self-invocation bypass it?

??? question "Reveal answer"
    Spring uses AOP proxies (JDK dynamic proxies or CGLIB subclassing) to intercept calls to `@Transactional` methods. The `TransactionInterceptor` wraps the invocation, begins a transaction via `PlatformTransactionManager`, executes the target method, and commits or rolls back based on the outcome.
    
    Self-invocation (`this.method()`) calls the method directly on the target instance in memory rather than through the Spring proxy, completely bypassing `TransactionInterceptor`.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q01TransactionalProxyMechanismExample.java"
        ```

### 2. What is the role of `PlatformTransactionManager` and `TransactionDefinition`?

??? question "Reveal answer"
    - `PlatformTransactionManager`: The central SPI in Spring transaction management providing methods `getTransaction(definition)`, `commit(status)`, and `rollback(status)`.
    - `TransactionDefinition`: Encapsulates transaction attributes including propagation behavior, isolation level, timeout, and read-only status.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q02PlatformTransactionManagerExample.java"
        ```

### 3. Compare `Propagation.REQUIRED` and `Propagation.REQUIRES_NEW`. What are the connection pool implications of each?

??? question "Reveal answer"
    - `Propagation.REQUIRED` (default): Participates in the existing transaction or creates a new one if none exists. Reuses the same physical database connection ($1$ connection).
    - `Propagation.REQUIRES_NEW`: Suspends the existing transaction and starts an autonomous new transaction. It acquires a **second physical database connection** from the pool on the same thread ($2$ connections held concurrently), risking connection pool deadlock under high load.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q03PropagationRequiredVsRequiresNewExample.java"
        ```

### 4. What are database isolation levels and which concurrency anomalies does each prevent?

??? question "Reveal answer"
    - `READ_UNCOMMITTED`: Allows dirty reads, non-repeatable reads, phantom reads.
    - `READ_COMMITTED`: Prevents dirty reads; allows non-repeatable reads and phantom reads.
    - `REPEATABLE_READ`: Prevents dirty reads and non-repeatable reads; allows phantom reads (prevented in MySQL InnoDB via MVCC and gap locks).
    - `SERIALIZABLE`: Prevents all anomalies by enforcing serial execution locking.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q04IsolationLevelsAnomaliesExample.java"
        ```

### 5. What are Spring's default rollback rules and how do checked exceptions behave?

??? question "Reveal answer"
    By default, Spring rolls back transactions **only on unchecked exceptions** (`RuntimeException` and `Error`).
    
    Checked exceptions (subclasses of `java.lang.Exception`) do **not** trigger a rollback; Spring commits the transaction by default. To roll back on checked exceptions, explicitly specify `@Transactional(rollbackFor = Exception.class)`.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q05RollbackRulesCheckedVsUncheckedExample.java"
        ```

### 6. What performance optimizations does `@Transactional(readOnly = true)` enable?

??? question "Reveal answer"
    1. **Hibernate Optimization**: Sets `FlushMode.MANUAL`, disabling dirty checking snapshots and reducing CPU/memory overhead during commit.
    2. **JDBC Driver Optimization**: Sets `Connection.setReadOnly(true)`, allowing database drivers and load balancers to route queries to read-replicas.
    3. **Database Engine Optimization**: Certain engines (e.g. Oracle, PostgreSQL) optimize MVCC snapshot isolation for read-only transactions.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q06ReadOnlyTransactionOptimizationExample.java"
        ```

### 7. When should you use `TransactionTemplate` over declarative `@Transactional`?

??? question "Reveal answer"
    `TransactionTemplate` provides programmatic transaction demarcation. Use it when:
    - You need fine-grained, tight transaction boundaries around specific lines of code without creating separate methods or classes.
    - You need to invoke transactional logic from within the same class without self-invocation proxy bypass issues.
    - You need explicit control over rollback and return values in complex algorithms.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q07TransactionTemplateProgrammaticExample.java"
        ```

### 8. How should HikariCP connection pools be sized in production?

??? question "Reveal answer"
    HikariCP pool sizing follows the PostgreSQL empirical formula:
    $$\text{Pool Size} = (\text{CPU Cores} \times 2) + \text{Effective Spindle Count}$$
    
    Over-allocating connections creates disk I/O thrashing, lock contention, and high context-switching overhead. A small, well-tuned pool (e.g. 10–20 connections) frequently outperforms a pool of hundreds of connections.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q08HikariCpPoolConfigurationExample.java"
        ```
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### 9. What is `TransactionSynchronizationManager` and how do lifecycle hooks work?

??? question "Reveal answer"
    `TransactionSynchronizationManager` manages `ThreadLocal` transaction state and registered `TransactionSynchronization` callbacks:
    - `beforeCommit(readOnly)`: Runs before physical database commit.
    - `afterCommit()`: Runs immediately after successful physical database commit.
    - `afterCompletion(status)`: Runs after commit or rollback completion for metric recording and resource cleanup.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q09TransactionSynchronizationManagerExample.java"
        ```

### 10. How does `@TransactionalEventListener` differ from standard `@EventListener`?

??? question "Reveal answer"
    Standard `@EventListener` executes immediately when `eventPublisher.publishEvent()` is called, regardless of active transaction state.
    
    `@TransactionalEventListener` binds event handling to transaction phases (`AFTER_COMMIT`, `AFTER_ROLLBACK`, `BEFORE_COMMIT`, `AFTER_COMPLETION`). If the outer transaction rolls back, `AFTER_COMMIT` listeners are discarded, preventing phantom external actions (e.g. emails, Kafka messages).

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q10TransactionalEventListenerPhasesExample.java"
        ```

### 11. How do transaction timeouts work and what happens when a query exceeds the timeout?

??? question "Reveal answer"
    The `timeout` attribute sets the maximum allowable execution duration (in seconds) for the transaction.
    
    Spring calculates the remaining time and sets `Statement.setQueryTimeout()` on executed JDBC statements. If the query exceeds the remaining duration, the JDBC driver cancels the statement and Spring throws `TransactionTimedOutException`.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q11TransactionTimeoutCancellationExample.java"
        ```

### 12. How does `Propagation.NESTED` work with JDBC Savepoints?

??? question "Reveal answer"
    `Propagation.NESTED` executes within the existing physical database connection using JDBC **Savepoints**.
    
    If the nested transaction fails, it rolls back changes only up to the savepoint, allowing the outer transaction to catch the exception and proceed with alternative logic or commit remaining work.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q12NestedPropagationSavepointsExample.java"
        ```

### 13. What causes `UnexpectedRollbackException` and how do you prevent it?

??? question "Reveal answer"
    When `ServiceA` calls `ServiceB` (both `Propagation.REQUIRED`), they share one physical transaction. If `ServiceB` throws a `RuntimeException`, Spring marks the transaction as **`rollback-only`**.
    
    If `ServiceA` catches `ServiceB`'s exception in a `try-catch` block and tries to commit, the transaction manager detects the `rollback-only` flag and throws `UnexpectedRollbackException`. To allow partial recovery, use `Propagation.REQUIRES_NEW` or `Propagation.NESTED`.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q13UnexpectedRollbackExceptionExample.java"
        ```

### 14. What happens when `@Async` is combined with `@Transactional`?

??? question "Reveal answer"
    `@Async` methods execute in a separate worker thread from a thread pool. Because Spring stores transaction state in `ThreadLocal` variables (`TransactionSynchronizationManager`), the calling transaction context is **not propagated** to the async thread.
    
    The async method will run in a separate transaction (or non-transactionally) and cannot participate in the caller's commit or rollback lifecycle.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q14AsyncMultithreadTransactionBoundaryExample.java"
        ```

### 15. How does HikariCP connection leak detection work?

??? question "Reveal answer"
    Setting `leakDetectionThreshold` (e.g. `5000` ms) instructs HikariCP to track open connections. If a thread holds a connection without closing it longer than the threshold, HikariCP logs a warning stack trace showing where the connection was acquired, pinpointing unclosed connections or excessively long transactions.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q15HikariConnectionLeakDetectionExample.java"
        ```

### 16. How does dynamic read-write splitting work with `AbstractRoutingDataSource`?

??? question "Reveal answer"
    `AbstractRoutingDataSource` dynamically selects a target `DataSource` at runtime.
    
    By overriding `determineCurrentLookupKey()`, it checks `TransactionSynchronizationManager.isCurrentTransactionReadOnly()`: routing read-only transactions to read replicas and write transactions to the primary database.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q16ReadOnlyRoutingDataSourceExample.java"
        ```
<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### 17. What are the limitations of `ChainedTransactionManager` across multiple databases?

??? question "Reveal answer"
    `ChainedTransactionManager` commits transactions sequentially in a best-effort 1PC pattern.
    
    It does **not** provide genuine 2PC/XA atomicity. If the first resource commits and the second resource fails during commit, the first commit cannot be rolled back, resulting in permanent data inconsistency across databases.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q17CustomTransactionManagerChainingExample.java"
        ```

### 18. Contrast Two-Phase Commit (2PC / XA) with the Saga pattern for distributed transactions.

??? question "Reveal answer"
    - **2PC / XA**: Synchronously coordinates resource managers with prepare and commit phases. Guarantees ACID consistency across databases, but holds distributed locks, creating high latency, single-point-of-failure risks, and low scalability.
    - **Saga Pattern**: Breaks a distributed transaction into a sequence of local transactions with compensating undo actions. Guarantees eventual consistency without holding global locks, offering high throughput and fault tolerance.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q18DistributedSagaVsTwoPhaseCommitExample.java"
        ```

### 19. How does `ThreadLocal` transaction state behave across thread boundaries and virtual threads?

??? question "Reveal answer"
    `TransactionSynchronizationManager` relies on standard `ThreadLocal` instances. When offloading work to executors, child threads do not inherit transaction state unless explicit `TaskDecorator`s or context copiers are configured.
    
    In Java 21 with Virtual Threads, `ThreadLocal` allocations scale efficiently, but `ScopedValue` is increasingly preferred for structured lexical scoping.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q19ThreadLocalTransactionStatePropagationExample.java"
        ```

### 20. How does the Transactional Outbox Pattern ensure atomic database updates and event publishing?

??? question "Reveal answer"
    The Transactional Outbox Pattern inserts both the business entity record and an outbox message record into the **same local database transaction**.
    
    A separate background process (or Change Data Capture via Debezium) polls or streams messages from the outbox table to Kafka/RabbitMQ with at-least-once delivery guarantees, avoiding dual-write inconsistencies.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q20TransactionalOutboxPatternExample.java"
        ```

### 21. How do you implement programmatic savepoint rollback using `TransactionStatus`?

??? question "Reveal answer"
    `TransactionStatus` provides `createSavepoint()`, `rollbackToSavepoint(savepoint)`, and `releaseSavepoint(savepoint)`.
    
    This allows developers to create granular recovery checkpoints within complex batch processing routines, rolling back individual record failures while preserving preceding operations in the same transaction.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q21ProgrammaticSavepointRollbackExample.java"
        ```
<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Production Scenarios

### 22. Production Incident: HikariCP connection pool starvation from slow third-party API in `@Transactional`. How do you diagnose and remediate?

??? question "Reveal answer"
    **Symptom**: All microservice endpoints start timing out with `HikariPool - Connection is not available, request timed out after 30000ms`. Database CPU and I/O remain near zero.
    
    **Root Cause**: Downstream third-party payment API latency spiked to 4.5 seconds. A controller service executed the remote HTTP call inside a `@Transactional` method, holding database connections hostage for the duration of the network call.
    
    **Remediation**:
    1. Extract remote HTTP calls outside `@Transactional` methods into an orchestrator service.
    2. Scope database transactions tightly around state transitions before and after the remote call.
    3. Configure `leakDetectionThreshold` on HikariCP to detect long-lived connection holders.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q22HikariPoolStarvationScenarioExample.java"
        ```

### 23. Data Incident: `UnexpectedRollbackException` and silent partial commit in multi-service propagation failure. How do you diagnose and remediate?

??? question "Reveal answer"
    **Symptom**: `UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only` logged in production; parent service believed order creation succeeded because it caught the exception.
    
    **Root Cause**: Parent service invoked child inventory service (both `Propagation.REQUIRED`). When inventory check threw a runtime exception, the inner transaction marked the global physical transaction `rollback-only`. The parent service caught the exception in a `try-catch` and attempted to commit.
    
    **Remediation**:
    1. If the child failure must allow the parent to continue, configure child method with `Propagation.REQUIRES_NEW` or `Propagation.NESTED` (with Savepoints).
    2. Otherwise, allow the exception to bubble up naturally without swallowing it.

    ??? example "Example"
        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q23UnexpectedRollbackMultiServiceScenarioExample.java"
        ```
<!-- --8<-- [end:scenarios] -->
