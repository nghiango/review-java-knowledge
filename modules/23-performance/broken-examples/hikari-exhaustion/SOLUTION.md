# Solution: checkout connection lifetime

## Annotated code

```java
package lab.performance.broken.connectionpool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

public final class CheckoutService {
    private final DataSource dataSource;
    private final PaymentGateway paymentGateway;

    public CheckoutService(DataSource dataSource, PaymentGateway paymentGateway) {
        this.dataSource = dataSource;
        this.paymentGateway = paymentGateway;
    }

    public void checkout(long orderId) throws SQLException {
        // Resource leak issue: The connection remains checked out across unrelated remote I/O.
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    "update orders set status = 'PAYING' where id = ?")) {
                statement.setLong(1, orderId);
                statement.executeUpdate();
            }
            // Transaction issue: An unpredictable payment call lengthens the database transaction.
            // Reliability issue: A successful charge followed by commit failure has no recovery path.
            paymentGateway.charge(orderId);
            connection.commit();
        }
    }

    public interface PaymentGateway {
        void charge(long orderId);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Resource leak issue | High | `checkout()` | Connection retained during payment I/O |
| 2 | Transaction issue | High | `checkout()` | Transaction duration follows remote latency |
| 3 | Reliability issue | High | `checkout()` | Charge and commit can diverge |

## Issue details

### Connection retained during payment I/O
**Type:** Resource leak issue · **Severity:** High · **Difficulty:** Intermediate  
**Technology:** HikariCP, JDBC · **Interview frequency:** High · **Production impact:** High

**Problem:** A scarce connection is idle while payment runs. **Why it happens:** resource scope
matches the whole method. **Production impact:** active reaches max, pending climbs, and requests
time out while DB CPU can remain low. **Correct implementation:** budget pools across instances and
keep transactions around DB work only. **Trade-offs:** the workflow needs explicit state and
reconciliation. **How to detect it:** Hikari active/pending metrics and thread dumps at
`getConnection()`. **Interview follow-up:** How would Little's Law estimate required concurrency?

### Remote call inside database transaction
**Type:** Transaction issue · **Severity:** High · **Difficulty:** Senior  
**Technology:** JDBC · **Interview frequency:** High · **Production impact:** High

**Problem:** Lock and transaction lifetimes inherit payment latency. **Why it happens:** the commit
is after the remote call. **Production impact:** longer locks and reduced pool throughput.
**Correct implementation:** commit a pending state, call payment, then finalize in a short second
transaction. **Trade-offs:** eventual consistency. **How to detect it:** transaction duration and
lock-wait metrics. **Interview follow-up:** Where is idempotency stored?

### Charge and commit can diverge
**Type:** Reliability issue · **Severity:** High · **Difficulty:** Senior  
**Technology:** payments, JDBC · **Interview frequency:** High · **Production impact:** High

**Problem:** payment can succeed before commit fails. **Why it happens:** two systems cannot share
this local transaction. **Production impact:** charged orders remain pending. **Correct
implementation:** idempotency plus durable reconciliation. **Trade-offs:** more workflow state.
**How to detect it:** compare gateway transactions with pending orders. **Interview follow-up:**
When would a Saga compensate?

## Correct implementation

Pool sizing lives in package `lab.performance.connectionpool`; the workflow boundary is explained
in [solutions](../../../docs/topics/performance/solutions.md#connection-pool-budget-and-short-resource-scopes).
