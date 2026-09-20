# JPA / Hibernate Practice Exercises

Put your ORM knowledge into practice with these hands-on engineering katas.

---

## Kata 1: Optimize an N+1 Query Cascade

### Objective
Given a legacy reporting service that issues hundreds of SQL queries to generate department employee reports, refactor the implementation using both:
1. Dynamic Entity Graphs (`@EntityGraph` / `fetchgraph`).
2. Type-safe DTO Projection with `JdbcClient` or JPQL Constructor Expressions.

### Verification Criteria
- Verify with SQL query counting assertions that exactly 1 query is executed.
- Ensure no lazy loading proxies leak outside the service boundary.

---

## Kata 2: High-Volume Batch Importer with `StatelessSession`

### Objective
Build a high-performance CSV batch importer capable of importing 100,000 transaction records in under 3 seconds.

### Constraints & Requirements
- Use PostgreSQL sequence pre-allocation (`allocationSize = 100`).
- Use Hibernate `StatelessSession` to eliminate 1st-level cache overhead and dirty checking.
- Enable JDBC batching (`batch_size = 100`).
- Write an integration test using Testcontainers to verify throughput and heap memory stability.

---

## Kata 3: Optimistic Locking Retry Mechanism

### Objective
Implement an e-commerce inventory decrement workflow with `@Version` optimistic locking.

### Requirements
- Handle `OptimisticLockException` using a Spring `@Retryable` backoff policy with exponential jitter.
- Compare throughput and retry success rates under high contention (50 concurrent threads attempting to purchase the last 5 items).
- Contrast results with a pessimistic locking implementation (`LockModeType.PESSIMISTIC_WRITE`).
