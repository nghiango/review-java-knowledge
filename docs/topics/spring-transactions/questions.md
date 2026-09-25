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
    $\displaystyle \text{Pool Size} = (\text{CPU Cores} \times 2) + \text{Effective Spindle Count}$
    
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

### 24. What is the contract of TransactionSynchronization lifecycle callbacks (beforeCommit, afterCommit, afterCompletion)?

??? question "Reveal answer"

    **Short Answer:** `TransactionSynchronization` provides lifecycle callbacks: `beforeCommit` executes before SQL `COMMIT` (exceptions cause rollback); `afterCommit` executes after successful commit (for external side-effects like sending emails); `afterCompletion` executes on both commit and rollback (for resource cleanup).

    **Internal Mechanism:** `TransactionSynchronizationManager` holds a `ThreadLocal<List<TransactionSynchronization>>` bound to the active transaction, which `AbstractPlatformTransactionManager` iterates through during commit and rollback operations.

    **Common Mistake:** Emitting external message broker events or REST calls inside `beforeCommit`; if the subsequent database commit fails or deadlocks, the message was already sent, causing dual-write inconsistency. [Concepts](/topics/spring-transactions/concepts.md#6-transactionaleventlistener-and-transaction-synchronization)

    ??? example "Example"

        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q24SynchronizationCallbackPhasesExample.java"
        ```

### 25. How do you design replication-lag safe read-write splitting with AbstractRoutingDataSource?

??? question "Reveal answer"

    **Short Answer:** Asynchronous database replication introduces lag between the Primary and Read Replicas. If a client writes and immediately reads on the replica, they see stale data. Guard against this using a "read-your-own-writes" window that routes reads to the Primary node for a short interval after any write.

    **Internal Mechanism:** `AbstractRoutingDataSource.determineCurrentLookupKey()` checks both `TransactionSynchronizationManager.isCurrentTransactionReadOnly()` and a user/session `lastWriteTimestamp`; if `(now - lastWrite) < LAG_WINDOW_MS`, reads are routed to the Primary.

    **Common Mistake:** Assuming `@Transactional(readOnly = true)` can always be routed to read replicas without accounting for read-your-own-writes consistency in customer-facing flows. [Concepts](/topics/spring-transactions/concepts.md#1-transaction-abstraction-architecture)

    ??? example "Example"

        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q25ReplicationLagSafeRoutingExample.java"
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

### 26. How does the Transactional Outbox Pattern guarantee atomic event emission and what are the trade-offs of CDC vs polling?

??? question "Reveal answer"

    **Short Answer:** Instead of publishing messages directly to a message broker during a database transaction, an event record is written into an `outbox` table in the same local ACID transaction. An asynchronous process (CDC or SQL poller) reads the outbox table and delivers events to Kafka/RabbitMQ.

    **Deep Explanation:** In dual-write architectures, saving to a database and publishing to Kafka cannot be coordinated atomically without 2PC. If the database commits and message publication fails, or vice versa, the system becomes permanently inconsistent. Writing an outbox record into the database table guarantees that business state and outgoing events commit atomically together.

    **Internal Mechanism:** 
    - **Polling Publisher**: Uses `SELECT ... FOR UPDATE SKIP LOCKED` on the outbox table and deletes/marks rows after publishing.
    - **Change Data Capture (CDC)**: Uses Debezium / PostgreSQL logical replication to tail the Write-Ahead Log (`pg_wal`) directly with zero polling overhead.

    **Example:** [Transactional Outbox Pattern](/topics/spring-transactions/concepts.md#1-transaction-abstraction-architecture).

    **Common Mistake:** Emitting messages directly to Kafka inside an `@Transactional` method or using an un-polled outbox table that grows indefinitely.

    **Production Consideration:** Use Debezium CDC for high-throughput microservices; use SQL polling (`SKIP LOCKED`) for simpler architectures without dedicated Kafka Connect infrastructure.

    **Follow-up Questions:**
    - How does Kafka achieve idempotent message production and exactly-once delivery semantics? See [Kafka: Idempotent Producer](/topics/kafka/questions.md)
    - How do distributed data patterns coordinate data consistency across multiple microservice databases? See [Distributed Data Patterns: Outbox and Sagas](/topics/distributed-data-patterns/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q26CdcOutboxWalVsPollerExample.java"
        ```

### 27. What causes heuristic hazards in XA Two-Phase Commit and why do modern microservices adopt Sagas?

??? question "Reveal answer"

    **Short Answer:** In XA/2PC, a coordinator coordinates multiple participants. If a network partition occurs after participants vote `PREPARED`, participants hold row and table locks indefinitely. If a participant times out and unilaterally rolls back while another commits, a Heuristic Hazard occurs, causing split-brain data corruption.

    **Deep Explanation:** Two-Phase Commit is an anti-availability protocol ($CP$ in CAP theorem). During Phase 1 (Prepare), database resources acquire exclusive locks. If the coordinator or network fails, locks remain held, stalling all concurrent traffic. Modern distributed systems favor the Saga pattern, which trades atomic isolation for eventual consistency through local transactions and compensating actions.

    **Internal Mechanism:** The XA protocol defines `XAException.XA_HEURHAZ` when a participant heuristic decision conflicts with the global coordinator outcome.

    **Example:** [Distributed transactions vs Sagas](/topics/spring-transactions/concepts.md#2-transaction-propagation-behaviors).

    **Common Mistake:** Assuming XA/2PC provides zero-risk reliability in cloud environments with ephemeral containers and network latency.

    **Production Consideration:** Adopt orchestration or choreography-based Sagas with idempotent compensating transactions and dead-letter queues.

    **Follow-up Questions:**
    - How do distributed systems resolve network partition split-brain scenarios? See [Distributed Systems: Consensus and Raft](/topics/distributed-systems/questions.md)
    - How does Resilience4j configure retry and fallback compensation handlers? See [Resilience: Fault Tolerance Patterns](/topics/resilience/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q27XaTwoPhaseCommitHeuristicHazardExample.java"
        ```

### 28. How does HikariCP detect connection leaks using leakDetectionThreshold and thread stack tracing?

??? question "Reveal answer"

    **Short Answer:** Setting `leakDetectionThreshold > 0` (e.g. 5000ms) causes HikariCP to schedule a `ProxyLeakTask` when a connection is borrowed. If the connection is not returned within the threshold, a warning log is emitted containing the stack trace of the thread that borrowed it.

    **Deep Explanation:** Connection leaks occur when code borrows a JDBC `Connection` from the pool (either directly or via JPA `EntityManager`) and fails to close it (e.g. missing `try-with-resources`, long-running remote HTTP call in `@Transactional`, or infinite loop). HikariCP's leak detector captures `Thread.currentThread().getStackTrace()` at acquisition time, giving engineers the exact class, method, and line number responsible.

    **Internal Mechanism:** When `connection.close()` is called, the scheduled `ScheduledFuture` running the `ProxyLeakTask` is cancelled; if the task executes before cancellation, it logs the leak warning.

    **Example:** [HikariCP connection leak detection](/topics/spring-transactions/concepts.md#5-hikaricp-connection-pool-lifecycle-sizing).

    **Common Mistake:** Setting `leakDetectionThreshold` lower than legitimate long-running batch transactions, producing false-positive leak warnings.

    **Production Consideration:** Set `leakDetectionThreshold` to 2–3 times the expected p99 transaction duration (e.g. 5000ms–10000ms) in staging and production to detect leaked connections before the pool starves.

    **Follow-up Questions:**
    - How is the optimal database connection pool size calculated using the HikariCP formula? See [Database / SQL: Connection Pool Sizing](/topics/database-sql/questions.md#19-how-do-you-calculate-optimal-database-connection-pool-sizing-hikaricp-formula)
    - What Micrometer pool metrics monitor pending connection acquisition threads? See [Observability: Connection Pool Telemetry](/topics/observability/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q28StatementClosureAndLeakTaskExample.java"
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

### 29. Production Incident: Silent partial commit and UnexpectedRollbackException from swallowed exceptions in nested transactions

??? question "Reveal answer"

    **Short Answer:** A service method called a collaborator method (both `Propagation.REQUIRED`); when the collaborator threw an exception, the physical transaction was marked `rollback-only`. The caller swallowed the exception in a `try-catch` block and attempted to return normally, triggering an `UnexpectedRollbackException` on commit.

    **Deep Explanation:** In Spring declarative transactions, all participating methods with `Propagation.REQUIRED` share the same physical database connection and transaction boundary. When an inner method encounters an uncaught runtime exception, Spring's transaction interceptor marks the underlying transaction `rollback-only`. If the outer caller catches the exception and attempts to commit, Spring throws `UnexpectedRollbackException` because it cannot fulfill the commit contract.

    **Internal Mechanism:** `AbstractPlatformTransactionManager.processCommit()` checks `status.isRollbackOnly()`; if true, it rolls back the physical transaction and throws `UnexpectedRollbackException`.

    **Example:** [Rollback rules and nested boundaries](/topics/spring-transactions/code-review.md).

    **Common Mistake:** Catching exceptions from collaborator services in a `try-catch` block without realizing the underlying physical transaction has already been irrevocably marked for rollback.

    **Production Consideration:** Use `Propagation.REQUIRES_NEW` if inner service failure should allow outer transactions to commit, or use programmatic Savepoints (`Propagation.NESTED`) to rollback only the inner work.

    **Follow-up Questions:**
    - How does the JPA PersistenceContext react when a transaction is marked rollback-only? See [JPA / Hibernate: Entity Lifecycle](/topics/jpa-hibernate/questions.md#2-what-are-the-four-entity-lifecycle-states-in-jpa-and-how-do-transitions-occur)
    - How do checked exceptions behave under default Spring rollback rules? See [Spring Transactions: Rollback Rules](/topics/spring-transactions/questions.md#5-what-are-springs-default-rollback-rules-and-how-do-checked-exceptions-behave)

    ??? example "Example"

        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q29PartialCommitSilentFailureScenarioExample.java"
        ```

### 30. Production Incident: HikariCP connection pool starvation caused by un-timed remote HTTP calls inside @Transactional

??? question "Reveal answer"

    **Short Answer:** A slow downstream microservice call placed inside an `@Transactional` method caused threads to hold borrowed database connections idle for seconds; during a traffic spike, all 10 HikariCP connections became exhausted, rejecting incoming requests with connection acquisition timeouts.

    **Deep Explanation:** A database connection is borrowed the moment the transaction begins and is held until the transaction commits or rolls back. If an application makes external HTTP calls, writes to message brokers, or performs CPU-intensive encryption inside `@Transactional`, the JDBC connection is held hostage despite being completely idle on the database engine.

    **Internal Mechanism:** HikariCP's `getConnection()` blocks waiting on a free connection; when `connectionTimeout` (default 30,000ms) elapses, it throws `SQLTransientConnectionException: HikariPool - Connection is not available`.

    **Example:** [Remote call in transaction](/topics/spring-transactions/code-review.md).

    **Common Mistake:** Annotating an entire controller or orchestrator method with `@Transactional` instead of scoping transactions tightly around database persistence operations.

    **Production Consideration:** Keep transactions as short as possible. Use `TransactionTemplate` or separate transactional services for database operations, and execute all external network I/O strictly outside transaction boundaries.

    **Follow-up Questions:**
    - How does Tomcat worker thread starvation interact with HikariCP connection pool exhaustion? See [Spring MVC: Tomcat Worker Starvation](/topics/spring-mvc/questions.md#29-production-incident-tomcat-http-thread-pool-starvation-caused-by-un-timed-synchronous-downstream-calls)
    - How do circuit breakers and timeouts isolate downstream latency spikes? See [Resilience: Fault Tolerance Patterns](/topics/resilience/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q30SlowRemoteCallPoolStarvationScenarioExample.java"
        ```

<!-- --8<-- [end:scenarios] -->
