# Transaction Issues

Transaction boundaries, propagation, isolation and resource-lifetime failures.

## Entries

### Self-Invocation Bypasses Transactional Proxy

**Type:** Architecture issue · **Severity:** Critical · **Difficulty:** Basic

Calling a `@Transactional` method internally from within the same Spring bean (`this.method()`) bypasses the Spring AOP dynamic proxy. As a result, no `TransactionInterceptor` is executed and database modifications run outside transactional control. Extract the transactional logic into a separate collaborator bean or use `TransactionTemplate`.

**Appears in:** `modules/07-spring-transactions/broken-examples/self-invocation`

### Remote HTTP API Call Inside Database Transaction

**Type:** Performance issue · **Severity:** Critical · **Difficulty:** Intermediate

Executing slow third-party HTTP/REST API calls inside a `@Transactional` boundary holds the acquired HikariCP database connection open for seconds. Under concurrent traffic, all database pool connections are saturated, causing cascading connection timeouts across unrelated services. Extract remote network calls outside database transactions into orchestrators.

**Appears in:** `modules/07-spring-transactions/broken-examples/remote-api-in-transaction`

### Checked Exception Default Commit Assumption

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

Spring's default rollback policy rolls back transactions only for unchecked exceptions (`RuntimeException` and `Error`). Throwing a checked `Exception` results in the transaction committing by default. Explicitly declare `@Transactional(rollbackFor = Exception.class)` or use domain `RuntimeException` hierarchies.

**Appears in:** `modules/07-spring-transactions/broken-examples/checked-exception-rollback`

### REQUIRES_NEW Misuse Causing HikariCP Pool Deadlock

**Type:** Reliability issue · **Severity:** Critical · **Difficulty:** Senior

Calling a method annotated with `Propagation.REQUIRES_NEW` from an active transaction suspends the outer transaction while holding its database connection, and attempts to acquire a second connection from the same pool. If concurrent threads exhaust the pool, all threads deadlock waiting for connections. Use `Propagation.REQUIRED` or asynchronous messaging.

**Appears in:** `modules/07-spring-transactions/broken-examples/wrong-propagation-requires-new`

### Asynchronous Execution Across Transaction Boundaries

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

Methods annotated with `@Async` execute in separate worker threads that do not inherit the caller's `ThreadLocal` transaction context. If the caller's transaction later rolls back, the asynchronous task has already executed (e.g. sending customer emails for rolled-back orders). Use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.

**Appears in:** `modules/07-spring-transactions/broken-examples/async-transaction-boundary`

### `@Transactional` on Private Method Silently Ignored

**Type:** Architecture issue · **Severity:** High · **Difficulty:** Basic

Spring AOP dynamic proxies (CGLIB and JDK dynamic proxies) only intercept `public` methods. Placing `@Transactional` on a `private` method is silently ignored by Spring at runtime without error, running without transactional atomicity. Declare methods `public` on Spring-managed beans.

**Appears in:** `modules/07-spring-transactions/broken-examples/transaction-on-private-method`

---

### Dual Write to Database and Message Broker (Dual Write Problem)

**Type:** Data consistency issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Spring Transactions, Kafka, PostgreSQL · **Interview frequency:** High · **Production impact:** Critical

Writing to both an RDBMS and a message broker (Kafka, RabbitMQ) within an application service method inevitably leads to state corruption because local database transactions cannot atomically coordinate with remote network protocols. If the broker call fails, the database rolls back, but if the database commit fails after publishing, phantom events are consumed downstream. Implement the Transactional Outbox pattern so business state mutations and outbox records commit in the same local database transaction.

**Appears in:** `modules/19-distributed-data-patterns/broken-examples/dual-write-db-broker`

## Related

- [Issue catalogue](index.md)
