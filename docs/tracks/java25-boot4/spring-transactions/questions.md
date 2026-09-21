# Interview Questions: Spring Transactions in Spring Boot 4

!!! info "Delta from baseline"
    Baseline questions in [`docs/topics/spring-transactions/questions.md`](../../../topics/spring-transactions/questions.md) cover `@Transactional` proxies, rollback rules, isolation levels, and propagation behaviors.
    These 13 questions focus on **Spring Boot 4 / Java 25 transaction dynamics**: virtual thread confinement, StructuredTaskScope interaction, ScopedValue coordination, and HikariCP connection pool economics.

---

### 1. How does Spring's `@Transactional` interact with Java 25 Virtual Threads?

??? question "Reveal answer"
    Spring transactions are thread-bound via `ThreadLocal` in `TransactionSynchronizationManager`. When a request executes on a virtual thread, Spring binds the JDBC `ConnectionHolder` to that specific virtual thread.
    
    The transactional boundary functions identically to platform threads, but with one critical operational distinction: because virtual threads are lightweight, applications can spawn tens of thousands of concurrent requests. If each holds a database connection, the HikariCP pool quickly exhausts unless connection holding time is kept strictly minimal.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/examples/java/lab/java25boot4/springtransactions/questions/Q01VirtualThreadTransactionBoundaryBasicsExample.java"
        ```

---

### 2. What happens if you open a `StructuredTaskScope` inside a `@Transactional` method?

??? question "Reveal answer"
    Forking subtasks with `scope.fork(...)` executes them on child virtual threads. Because Spring transaction resources are stored in `ThreadLocal`, the child virtual threads **do not inherit the active database transaction**.
    
    If child subtasks execute SQL operations, they either:
    1. Acquire a separate database connection from the pool, running in auto-commit mode.
    2. Commit their changes independently of the parent transaction.
    If the parent transaction later rolls back, the child subtask changes remain committed, violating atomicity.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/examples/java/lab/java25boot4/springtransactions/questions/Q05StructuredTaskScopeInsideTransactionTrapExample.java"
        ```

---

### 3. How should `TransactionTemplate` be used in virtual thread architectures?

??? question "Reveal answer"
    In high-throughput virtual thread services, `TransactionTemplate` is preferred over broad class-level `@Transactional` annotations:
    - It confines the transaction boundary strictly to SQL statements, minimizing connection lease duration.
    - Non-database operations (JSON deserialization, remote HTTP verification, cryptographic hashing) execute before or after the template execution, releasing database connections back to the pool immediately.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/examples/java/lab/java25boot4/springtransactions/questions/Q02TransactionTemplateVirtualThreadsExample.java"
        ```

---

### 4. Does transaction propagation (`REQUIRED`, `REQUIRES_NEW`) span threads?

??? question "Reveal answer"
    No. Transaction propagation rules apply strictly within the thread that called the transactional method. When execution crosses a thread boundary (e.g. `CompletableFuture`, `@Async`, virtual thread executors, or structured scopes), propagation rules do not apply, and the child thread starts with no active transaction context.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/examples/java/lab/java25boot4/springtransactions/questions/Q03PropagationAcrossThreadsExample.java"
        ```

---

### 5. How does `@TransactionalEventListener(phase = AFTER_COMMIT)` behave with virtual threads?

??? question "Reveal answer"
    Events published inside `@Transactional` are held by `TransactionSynchronizationManager` until the physical database commit completes successfully.
    
    When configured with `@Async` running on virtual threads, the event listener is dispatched onto a separate virtual thread only after commit. This ensures that downstream notifications (emails, Kafka events, webhook calls) never trigger if the transaction rolls back.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/examples/java/lab/java25boot4/springtransactions/questions/Q04TransactionalEventListenerVirtualThreadExample.java"
        ```

---

### 6. Why should HikariCP maximumPoolSize NOT be increased to match virtual thread concurrency?

??? question "Reveal answer"
    Virtual threads scale based on JVM memory, allowing 100,000+ threads. However, database servers (PostgreSQL, MySQL) are bounded by CPU cores, disk I/O, and lock contention.
    
    Setting HikariCP pool size to thousands of connections overwhelms the database server, causing context switching thrashing and disk saturation. HikariCP should remain sized to database hardware capacity ($2 \times \text{cores} + \text{effective spindles}$), and applications should use backpressure or rate limiting rather than inflating connection pools.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/examples/java/lab/java25boot4/springtransactions/questions/Q06HikariPoolSizingVirtualThreadsExample.java"
        ```

---

### 7. What is connection lease duration and why is it critical in virtual thread architectures?

??? question "Reveal answer"
    Connection lease duration is the total elapsed time between `connection.getConnection()` and `connection.close()`.
    
    When 50,000 virtual threads run concurrently with a connection pool of 30:
    - If each transaction holds a connection for 500ms (due to slow network calls inside `@Transactional`), the pool can only serve 60 requests/sec, causing immediate starvation.
    - If the transaction holds the connection for only 5ms (pure database execution), the pool serves 6,000 requests/sec.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/examples/java/lab/java25boot4/springtransactions/questions/Q07ConnectionLeaseDurationVirtualThreadsExample.java"
        ```

---

### 8. How does `TransactionSynchronizationManager` determine transaction status?

??? question "Reveal answer"
    It inspects thread-local storage flags:
    - `isActualTransactionActive()`: returns `true` if an active physical transaction exists for the calling thread.
    - `isSynchronizationActive()`: returns `true` if transaction synchronizations (callbacks, event listeners) are registered.
    Because these are stored in `NamedThreadLocal`, any virtual thread spawned outside the proxy boundary will observe `false` for both checks.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/examples/java/lab/java25boot4/springtransactions/questions/Q08TransactionSynchronizationVirtualThreadExample.java"
        ```

---

### 9. What breaking changes affect transaction management when migrating Boot 3.5 → Boot 4.0?

??? question "Reveal answer"
    While core `@Transactional` semantics remain consistent:
    1. **Jakarta EE 11 / JPA 3.2 Baseline**: Hibernate 7 enforces stricter validation on entity mutations outside transaction boundaries.
    2. **Removal of Obsolete Deprecations**: Removed deprecated transaction definition constants and legacy transaction management configurations.
    3. **Observability Integration**: Spring Boot 4 unifies observation tracing directly into transaction lifecycles via Micrometer Observation.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/examples/java/lab/java25boot4/springtransactions/questions/Q09MigrationBoot3ToBoot4TransactionBehaviorExample.java"
        ```

---

### 10. How do you migrate transaction correlation metadata from `ThreadLocal` to `ScopedValue`?

??? question "Reveal answer"
    Migration steps:
    1. Replace `private static final ThreadLocal<TxContext> CONTEXT` with `private static final ScopedValue<TxContext> CONTEXT = ScopedValue.newInstance()`.
    2. Eliminate manual `try-finally` blocks containing `CONTEXT.remove()`.
    3. Bind the transaction context via `ScopedValue.where(CONTEXT, ctx).run(...)`.
    4. Virtual subtasks spawned in `StructuredTaskScope` automatically inherit `CONTEXT` with zero memory copy overhead.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/examples/java/lab/java25boot4/springtransactions/questions/Q10MigrationThreadLocalToScopedValueTransactionExample.java"
        ```

---

### 11. When should you choose Saga patterns over database transactions in virtual thread architectures?

??? question "Reveal answer"
    Saga patterns (orchestrated or choreographed compensating transactions) must be used whenever a business process spans multiple microservices, third-party APIs, or long-running parallel workflows.
    
    Holding an open database transaction across multiple remote virtual tasks causes database connection exhaustion and locks table rows against concurrent operations. Sagas break the workflow into independent, atomic local database transactions paired with compensating actions.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/examples/java/lab/java25boot4/springtransactions/questions/Q11DistributedSagaOverVirtualThreadsExample.java"
        ```

---

### 12. Are savepoints shared across virtual threads in Spring transactions?

??? question "Reveal answer"
    No. Savepoints are created on a specific JDBC `Connection` instance bound to the thread executing the transaction. Because connections cannot be safely accessed concurrently by multiple threads, savepoint creation, rollback, and release operations are strictly thread-confined.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/examples/java/lab/java25boot4/springtransactions/questions/Q12SavepointRollbackVirtualThreadSafetyExample.java"
        ```

---

### 13. A service running on virtual threads crashes with HikariCP `ConnectionTimeoutException`. How do you triage the incident?

??? question "Reveal answer"
    Investigation steps:
    1. **Inspect Thread Dumps**: Verify what virtual threads are executing while holding connections. Look for threads in `WAITING` or `TIMED_WAITING` doing remote HTTP calls or thread sleeps inside `@Transactional` methods.
    2. **Check HikariCP Metrics**: Monitor `HikariPool-1.pool.ActiveConnections` and `PendingThreads`. If active connections are pinned at maximum while throughput drops, connection holding time is the root cause.
    3. **Enable Leak Detection**: Set `spring.datasource.hikari.leak-detection-threshold=2000` to log stack traces of threads holding connections longer than 2 seconds.
    4. **Refactor Code**: Move all non-database I/O outside `@Transactional` boundaries using orchestrators or `TransactionTemplate`.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/07-spring-transactions/src/examples/java/lab/java25boot4/springtransactions/questions/Q13ScenarioHikariPoolExhaustionVirtualThreadsExample.java"
        ```
