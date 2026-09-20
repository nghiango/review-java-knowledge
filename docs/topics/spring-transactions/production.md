# Spring Transactions in Production

Production operations, incident diagnosis, connection pool tuning, and disaster avoidance for Spring transactions.

## 1. Diagnosing HikariCP Connection Pool Exhaustion

Connection pool starvation is the #1 database-related production incident in microservice architectures.

### Incident Symptoms
- Services stop processing incoming requests; logs show `HikariPool - Connection is not available, request timed out after 30000ms`.
- Database server CPU and disk utilization remain very low (<10%), while application thread dumps show hundreds of threads in `WAITING` on `HikariDataSource.getConnection()`.

### Root Cause Analysis
1. **Remote HTTP Calls Inside `@Transactional`**: Calling external payment gateways, credit check APIs, or email services inside a database transaction holds the database connection open for the duration of the network roundtrip.
2. **Too-Wide Transaction Boundaries**: Methods opening transactions at the controller or high-level service layer before data transformation or validation.
3. **Misconfigured `REQUIRES_NEW`**: Suspending outer transactions while acquiring second connections from the same pool.

### Diagnostic Checklist & Remediation
- Set `leakDetectionThreshold: 5000` in HikariCP to log stack traces of long-lived connection holders.
- Extract remote network I/O into non-transactional orchestrator classes.
- Scope database updates into short, atomic transactions.

---

## 2. Diagnosing Silent Rollback Failures & Unexpected Commits

### 1. Checked Exception Default Behavior
Spring transactions do **not** roll back on checked exceptions by default. If a method throws `IOException` or a custom `Exception`, Spring commits any database writes performed prior to the exception.
- **Remediation**: Always specify `@Transactional(rollbackFor = Exception.class)` or design custom exceptions as subclasses of `RuntimeException`.

### 2. Swallowing Exceptions from `Propagation.REQUIRED`
When an inner method throws a `RuntimeException`, Spring marks the underlying physical transaction as `rollback-only`. If the outer caller catches the exception in a `try-catch` block and attempts to commit, Spring rolls back the transaction and throws `UnexpectedRollbackException`.
- **Remediation**: If inner failures should not abort the outer transaction, configure the inner method with `Propagation.REQUIRES_NEW` (with pool sizing considerations) or `Propagation.NESTED` (JDBC savepoints).

---

## 3. Production HikariCP Sizing & Pool Optimization

### Empirical Sizing Formula
$$\text{Pool Size} = (\text{CPU Cores} \times 2) + \text{Effective Spindle Count}$$

For a database server with 8 CPU cores and SSD storage, a pool size of $16 - 20$ connections per application instance is often optimal.

### Key HikariCP Configuration Properties
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000       # 30 seconds to acquire connection
      idle-timeout: 600000             # 10 minutes
      max-lifetime: 1800000            # 30 minutes (must be < DB wait_timeout)
      leak-detection-threshold: 5000   # 5 seconds to detect leaks
```

---

## 4. Production Spring Transactions Checklist

- [ ] Zero remote HTTP, gRPC, or external API calls inside `@Transactional` methods.
- [ ] All `@Transactional` methods are `public` and invoked across collaborator bean boundaries.
- [ ] Methods throwing checked exceptions declare `rollbackFor = Exception.class`.
- [ ] Read-only query methods declare `@Transactional(readOnly = true)` to disable dirty checking.
- [ ] External side effects (emails, Kafka messages, webhooks) are dispatched via `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
- [ ] HikariCP `leakDetectionThreshold` is configured in production monitoring.
- [ ] `Propagation.REQUIRES_NEW` is audited to ensure the connection pool size exceeds peak concurrent threads $\times 2$.
