# Solution: Lost Update Without Locking

## Annotated Code

### `BankAccountService.java`
```java
package lab.databasesql.broken.locking;

import java.math.BigDecimal;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankAccountService {

    private final JdbcClient jdbcClient;

    public BankAccountService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public record AccountBalance(Long id, BigDecimal balance) {}

    @Transactional
    public void withdraw(Long accountId, BigDecimal amount) {
        // Concurrency issue: Read-Modify-Write in application memory without row-level lock (SELECT FOR UPDATE), optimistic versioning, or atomic update causes Lost Update anomalies under concurrent execution
        BigDecimal currentBalance =
                jdbcClient
                        .sql("SELECT balance FROM bank_accounts WHERE id = :id")
                        .param("id", accountId)
                        .query(BigDecimal.class)
                        .single();

        if (currentBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        BigDecimal newBalance = currentBalance.subtract(amount);

        jdbcClient
                .sql("UPDATE bank_accounts SET balance = :newBalance WHERE id = :id")
                .param("newBalance", newBalance)
                .param("id", accountId)
                .update();
    }
}
```

---

## Issues Found

| Issue | Severity | Category | Description |
|---|---|---|---|
| Lost Update Anomaly | Critical | Concurrency | Concurrent threads executing the read-modify-write cycle simultaneously will overwrite each other's balance updates, causing direct financial discrepancy and data loss. |

---

## Remediation Strategy

1. **Option 1: Atomic Database Update (Preferred for simple arithmetic):**
   ```sql
   UPDATE bank_accounts 
   SET balance = balance - :amount 
   WHERE id = :id AND balance >= :amount
   ```
   Check the affected row count: if `0`, throw `InsufficientFundsException`.
2. **Option 2: Pessimistic Row Lock (`SELECT FOR UPDATE`):**
   ```sql
   SELECT balance FROM bank_accounts WHERE id = :id FOR UPDATE
   ```
   Acquires an exclusive row lock, serializing concurrent transactions on the same account.
3. **Option 3: Optimistic Locking with Version Column:**
   ```sql
   UPDATE bank_accounts 
   SET balance = :newBalance, version = version + 1 
   WHERE id = :id AND version = :version
   ```
