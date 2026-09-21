# Internals: Transaction Synchronization & Virtual Thread Confinement

!!! info "Delta from baseline"
    Baseline internals in [`docs/topics/spring-transactions/internals.md`](../../../topics/spring-transactions/internals.md) detail `TransactionInterceptor`, `PlatformTransactionManager`, and `TransactionSynchronizationManager`.
    This page covers **internal state storage and virtual thread scheduling**: why transaction synchronization remains thread-confined, connection holder lifecycles, and thread hops.

---

## 1. `TransactionSynchronizationManager` Internal Storage

At the core of Spring transaction management lies `TransactionSynchronizationManager`:

```java
public abstract class TransactionSynchronizationManager {
    private static final ThreadLocal<Map<Object, Object>> resources =
            new NamedThreadLocal<>("Transactional resources");

    private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
            new NamedThreadLocal<>("Transaction synchronizations");

    private static final ThreadLocal<String> currentTransactionName =
            new NamedThreadLocal<>("Current transaction name");
    ...
}
```

Key characteristics:
1. **`NamedThreadLocal` Storage**: Resources (such as JDBC `ConnectionHolder`, Hibernate `SessionHolder`) are stored in a `ThreadLocal` keyed by `DataSource`.
2. **Thread Confinement**: When a thread calls `DataSourceUtils.getConnection(dataSource)`, it checks if `resources.get()` contains a connection for this `DataSource`. If present, that exact connection is returned.
3. **No Automatic Inheritance**: Neither platform nor virtual threads inherit `NamedThreadLocal` across thread hops or executors.

---

## 2. What Happens During Structured Concurrency

When calling `StructuredTaskScope.open()` inside a `@Transactional` block:

```mermaid
graph TD
    ParentTx["Parent Request Thread<br/>Transaction Active = true<br/>ConnectionHolder = Conn #1"]
    ParentTx -->|scope.fork()| Child1["Child Virtual Thread 1<br/>ThreadLocal empty!"]
    ParentTx -->|scope.fork()| Child2["Child Virtual Thread 2<br/>ThreadLocal empty!"]
    Child1 -->|Acquires Conn #2| DB[(PostgreSQL)]
    Child2 -->|Acquires Conn #3| DB
```

Because `Child1` and `Child2` execute on distinct virtual threads:
- Their `TransactionSynchronizationManager.isActualTransactionActive()` returns `false`.
- Database operations trigger independent connection acquisition, bypassing transaction management completely or creating nested, uncoordinated auto-commits.
- A failure in `ParentTx` rolls back `Conn #1` only; `Conn #2` and `Conn #3` are untouched.
