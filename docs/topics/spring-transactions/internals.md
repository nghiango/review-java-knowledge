# Spring Transactions Internals

A deep dive into the internal execution mechanics of Spring's transaction interceptor, connection binding in `TransactionSynchronizationManager`, proxy generation, and commit/rollback sequence.

## 1. Transaction Interception Pipeline

When a client invokes a method on a transactional Spring bean, the call enters the `TransactionInterceptor`:

```mermaid
sequenceDiagram
    autonumber
    actor Caller
    participant Proxy as CGLIB / JDK Proxy
    participant TI as TransactionInterceptor
    participant TM as PlatformTransactionManager
    participant TSM as TransactionSynchronizationManager
    participant DS as HikariDataSource
    participant Target as Target Service Bean

    Caller->>Proxy: placeOrder()
    Proxy->>TI: invoke(MethodInvocation)
    TI->>TI: createTransactionIfNecessary()
    TI->>TM: getTransaction(definition)
    
    opt New Transaction
        TM->>DS: getConnection()
        DS-->>TM: java.sql.Connection
        TM->>TSM: bindResource(dataSource, connectionHolder)
        TM->>TSM: initSynchronization()
    end

    TI->>Target: proceed() (Execute Business Logic)
    
    alt Method throws Exception
        Target-->>TI: Exception thrown
        TI->>TI: completeTransactionAfterThrowing()
        alt Exception matches rollback rule
            TI->>TM: rollback(status)
            TM->>TSM: triggerAfterCompletion(STATUS_ROLLED_BACK)
        else Exception does not match (Checked Exception)
            TI->>TM: commit(status)
            TM->>TSM: triggerAfterCommit()
        end
    else Normal Execution
        Target-->>TI: Return Value
        TI->>TI: commitTransactionAfterReturning()
        TI->>TM: commit(status)
        TM->>TSM: triggerBeforeCommit()
        TM->>TM: connection.commit()
        TM->>TSM: triggerAfterCommit()
        TM->>TSM: triggerAfterCompletion(STATUS_COMMITTED)
    end

    TI->>TSM: unbindResource(dataSource)
    TI->>DS: connection.close() (Return to Pool)
    TI-->>Caller: Result
```

---

## 2. `TransactionSynchronizationManager` and `ThreadLocal` State

Spring manages transaction resources per-thread using `ThreadLocal` storage in `TransactionSynchronizationManager`:

- **`resources`** (`ThreadLocal<Map<Object, Object>>`): Maps `DataSource` or `EntityManagerFactory` to `ConnectionHolder` or `EntityManagerHolder`.
- **`synchronizations`** (`ThreadLocal<Set<TransactionSynchronization>>`): Ordered set of callbacks (`beforeCommit`, `afterCommit`, `afterCompletion`).
- **`currentTransactionName`** (`ThreadLocal<String>`): Method signature or transaction qualifier name.
- **`currentTransactionReadOnly`** (`ThreadLocal<Boolean>`): Flag for read-only optimization.
- **`actualTransactionActive`** (`ThreadLocal<Boolean>`): True when a physical transaction is active.

---

## 3. Propagation Resolution & Suspension

When an inner method executes with different propagation:

```mermaid
flowchart TD
    Invoke[Method Invocation] --> Check{Active Transaction Exists?}
    Check -->|No| CreateNew[Create New Transaction]
    Check -->|Yes| Prop{Propagation Type}

    Prop -->|REQUIRED| Join[Join Existing Transaction]
    Prop -->|SUPPORTS| Join
    Prop -->|MANDATORY| Join
    Prop -->|REQUIRES_NEW| Suspend[Suspend Outer: Unbind Connection -> Start New Transaction]
    Prop -->|NOT_SUPPORTED| SuspendNoTx[Suspend Outer -> Run Non-Transactionally]
    Prop -->|NEVER| ThrowEx[Throw IllegalTransactionStateException]
    Prop -->|NESTED| Savepoint[Create JDBC Savepoint on Existing Connection]
```

### Suspension Mechanics in `REQUIRES_NEW`
1. `TransactionSynchronizationManager.unbindResource(dataSource)` removes the outer `ConnectionHolder` from `ThreadLocal`.
2. A `SuspendedResourcesHolder` preserves the outer connection, synchronizations, and transaction name.
3. The inner transaction acquires a **second physical connection** from HikariCP and binds it to `ThreadLocal`.
4. When the inner transaction completes, it commits, closes the second connection, and restores the suspended outer connection.

---

## 4. Why `UnexpectedRollbackException` Occurs

When two methods participate in the same physical transaction (`Propagation.REQUIRED`):

1. Outer `ServiceA` calls inner `ServiceB`.
2. `ServiceB` throws a `RuntimeException`.
3. `TransactionInterceptor` catches the exception in `ServiceB` and marks the physical `TransactionStatus` as **`rollback-only`**.
4. If `ServiceA` catches `ServiceB`'s exception in a `try-catch` block and attempts to complete normally, `TransactionInterceptor` attempts to commit.
5. The `PlatformTransactionManager` detects the `rollback-only` flag on the physical transaction.
6. It rolls back the database transaction and throws `UnexpectedRollbackException` to notify `ServiceA` that its requested commit was denied.
