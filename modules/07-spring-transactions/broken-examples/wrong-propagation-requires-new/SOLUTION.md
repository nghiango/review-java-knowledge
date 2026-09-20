# Solution: Wrong Propagation and Connection Pool Exhaustion

## Annotated Code

```java
package lab.springtransactions.broken.propagation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    // Reliability issue: REQUIRES_NEW inside active transaction acquires 2nd DB connection on same thread, risking pool deadlock
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAudit(String orderId, String action) {
        if ("FAIL_AUDIT".equals(action)) {
            throw new RuntimeException("Audit system unreachable");
        }
        // DB insert into audit_log
    }
}
```

```java
package lab.springtransactions.broken.propagation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final AuditLogService auditLogService;

    public OrderService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void createOrder(String orderId, double amount) {
        insertOrder(orderId, amount);

        try {
            // Reliability issue: suspending outer transaction holds 1st connection while waiting for 2nd connection
            auditLogService.recordAudit(orderId, "ORDER_CREATED");
        } catch (Exception e) {
            // Catching exception from inner transaction
        }
    }

    private void insertOrder(String orderId, double amount) {
        // DB insert
    }
}
```

## Issues Identified

### Reliability issue: REQUIRES_NEW inside active transaction causes HikariCP connection pool deadlock
- **Location:** `AuditLogService.java#recordAudit`
- **Explanation:** When `OrderService.createOrder` executes, it acquires connection $C_1$ from the Hikari pool. When it calls `AuditLogService.recordAudit` with `Propagation.REQUIRES_NEW`, Spring suspends the outer transaction (keeping $C_1$ held) and attempts to acquire a second connection $C_2$ from the pool for the inner transaction. If the connection pool has $N$ connections and $N$ concurrent threads enter `createOrder`, all $N$ threads hold $C_1$ and block waiting for a free connection for $C_2$, resulting in a complete pool deadlock until connection timeouts fire.

## Correct implementation

See `lab.springtransactions.propagation.OrderService` and `lab.springtransactions.propagation.AuditLogService`.
