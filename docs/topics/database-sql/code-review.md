# Database / SQL Code Review Challenges

Review the following 7 real-world database and SQL pull requests containing critical performance bottlenecks, indexing mistakes, concurrency race conditions, and migration hazards.

---

## 1. Unindexed Foreign Key and Missing Index

### Context
An e-commerce order management system stores `orders` and `order_items`. The schema defines a foreign key constraint on `order_items.order_id REFERENCES orders(id) ON DELETE CASCADE`.

### Clean Code Under Review

```sql
--8<-- "modules/09-database-sql/broken-examples/missing-index-unindexed-foreign-key/schema.sql"
```

```java
--8<-- "modules/09-database-sql/broken-examples/missing-index-unindexed-foreign-key/OrderRepository.java"
```

### Review Questions
1. Why does PostgreSQL not automatically index foreign key columns when a constraint is defined?
2. What happens to query execution plans and lock acquisitions when an order is deleted from `orders`?

---

## 2. Wrong Composite Index Column Order

### Context
A multi-tenant platform logs audit events into `audit_logs`. The query filters by `tenant_id` and `created_at`. The developer created an index on `(status, created_at, tenant_id)`.

### Clean Code Under Review

```sql
--8<-- "modules/09-database-sql/broken-examples/wrong-composite-index-order/schema.sql"
```

```java
--8<-- "modules/09-database-sql/broken-examples/wrong-composite-index-order/AuditLogRepository.java"
```

### Review Questions
1. Why does the leftmost prefix rule prevent this index from being used effectively when `status` is omitted?
2. How should composite index columns be ordered following the equality-then-range principle?

---

## 3. Deep Offset Pagination Performance

### Context
A banking ledger API provides transaction history pagination using `LIMIT :pageSize OFFSET :offset`.

### Clean Code Under Review

```java
--8<-- "modules/09-database-sql/broken-examples/offset-pagination-performance/TransactionSearchService.java"
```

### Review Questions
1. Why does `OFFSET 500000` exhibit linear $O(N)$ latency degradation?
2. How does keyset pagination ensure constant $O(1)$ response time and eliminate pagination drift?

---

## 4. Lost Update Without Locking

### Context
An account management service executes bank withdrawals using application-level read-modify-write logic.

### Clean Code Under Review

```java
--8<-- "modules/09-database-sql/broken-examples/lost-update-without-locking/BankAccountService.java"
```

### Review Questions
1. What race condition occurs when two concurrent requests withdraw from the same account simultaneously?
2. Why is single-statement atomic update (`UPDATE ... WHERE balance >= :amount`) preferred over application locking?

---

## 5. Long Transaction Holding Locks

### Context
An invoice batch processing worker holds an exclusive database row lock (`SELECT FOR UPDATE`) while making slow external API calls.

### Clean Code Under Review

```java
--8<-- "modules/09-database-sql/broken-examples/long-transaction-holding-locks/InvoiceProcessingService.java"
```

### Review Questions
1. What impact does holding database row locks during remote network I/O have on connection pools and concurrent readers?
2. How should the transaction boundary be structured using a state machine pattern?

---

## 6. SQL Injection via String Concatenation

### Context
A user administration portal dynamically searches users by concatenating search filter strings directly into SQL statements.

### Clean Code Under Review

```java
--8<-- "modules/09-database-sql/broken-examples/sql-injection-string-concatenation/UserSearchController.java"
```

### Review Questions
1. How can an attacker exploit `queryParam` to execute arbitrary SQL or extract all user credentials?
2. What are the performance drawbacks of dynamic SQL string concatenation on database query plan caches?

---

## 7. Destructive Database Migration

### Context
A Flyway migration renames `full_address` to `delivery_address` in a single SQL statement during active rolling deployment.

### Clean Code Under Review

```sql
--8<-- "modules/09-database-sql/broken-examples/destructive-database-migration/V2__rename_customer_address_column.sql"
```

```java
--8<-- "modules/09-database-sql/broken-examples/destructive-database-migration/CustomerRepository.java"
```

### Review Questions
1. What runtime errors will occur on active V1 application instances during rolling deployment?
2. How does the 4-phase Expand and Contract pattern guarantee zero downtime?
