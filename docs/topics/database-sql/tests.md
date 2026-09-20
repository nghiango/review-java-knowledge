# Database / SQL Testing Strategies

## Testing Architecture

Testing database and SQL components involves verifying parameterized query generation in unit tests and validating real PostgreSQL execution plans, locks, and constraints in integration tests via Testcontainers.

| Test Type | Focus Area | Infrastructure | Speed | Tooling |
|---|---|---|---|---|
| **Unit Tests** | SQL parameter binding, DTO mapping, business logic | In-memory / Mockito | Fast (< 50ms) | JUnit 5, Mockito, AssertJ |
| **Slice Tests** | Query syntax, JDBC mapping, transaction boundaries | Testcontainers PostgreSQL | Fast (~ 1-2s) | `@JdbcTest`, Testcontainers |
| **Concurrency Tests** | Lost update prevention, deadlocks, `SKIP LOCKED` | Testcontainers PostgreSQL | Thorough (~ 3-5s) | JUnit 5, Multi-threaded runners |

---

## Unit Test Implementations

### 1. Keyset Pagination Test
```java
--8<-- "modules/09-database-sql/src/test/java/lab/databasesql/pagination/TransactionPaginationServiceTest.java"
```

---

### 2. Atomic Balance Modification Test
```java
--8<-- "modules/09-database-sql/src/test/java/lab/databasesql/locking/BankAccountServiceTest.java"
```

---

### 3. State Machine Transaction Scope Test
```java
--8<-- "modules/09-database-sql/src/test/java/lab/databasesql/transactionscope/InvoiceProcessingServiceTest.java"
```

---

### 4. Parameterized SQL Injection Safety Test
```java
--8<-- "modules/09-database-sql/src/test/java/lab/databasesql/safequeries/CustomerSearchServiceTest.java"
```
