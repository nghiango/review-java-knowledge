# Production: Transaction Operations & Connection Economics

!!! info "Delta from baseline"
    Baseline production operations in [`docs/topics/spring-transactions/production.md`](../../../topics/spring-transactions/production.md) cover HikariCP leak detection, slow query logs, and transaction timeouts.
    This page covers **connection economics in virtual thread architectures on Spring Boot 4**: connection pool sizing, connection lease duration, and preventing pool starvation.

---

## 1. Connection Economics with Virtual Threads

The most dangerous pitfall when enabling virtual threads in Spring Boot (`spring.threads.virtual.enabled=true`) is assuming the database connection pool can scale with virtual threads:

| Component | Concurrency Capability | Bottleneck Factor |
|---|---|---|
| **Virtual Threads** | 100,000+ active threads | Cheap JVM continuation memory (~few KB) |
| **HikariCP Connection Pool** | 20 – 100 connections | Database engine CPU cores, disk I/O, lock contention |

If 10,000 virtual threads each enter a `@Transactional` method that performs slow I/O or blocks on external APIs, all 20 connections in the HikariCP pool are leased within milliseconds. The remaining 9,980 virtual threads block waiting for connections, triggering catastrophic connection timeout cascading failures across the cluster.

---

## 2. Best Practices for Connection Pool Sizing

1. **Keep HikariCP Pool Sized to DB Core Capacity**:
   $$\text{maximumPoolSize} = 2 \times \text{CPU Cores} + \text{effective\_spindle\_count}$$
2. **Minimize Connection Lease Duration**:
   - Never hold a database connection across external network calls.
   - Use `TransactionTemplate` to bound the transaction strictly around SQL execution.
3. **Configure Connection Timeout & Leak Detection**:
   ```properties
   spring.datasource.hikari.connection-timeout=3000
   spring.datasource.hikari.leak-detection-threshold=2000
   ```
