# Solution — Dual-Write Consistency & Transaction Ordering

## Annotated code

```java
package lab.cachingredis.broken.dualwrite;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletTransferService {

    private final Map<String, BigDecimal> databaseAccounts = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;

    public WalletTransferService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        databaseAccounts.put("acc-101", new BigDecimal("1000.00"));
        databaseAccounts.put("acc-102", new BigDecimal("500.00"));
    }

    @Transactional
    public void transferFunds(String fromAccount, String toAccount, BigDecimal amount) {
        BigDecimal senderBal = databaseAccounts.get(fromAccount);
        if (senderBal == null || senderBal.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds in account: " + fromAccount);
        }

        BigDecimal receiverBal = databaseAccounts.get(toAccount);
        if (receiverBal == null) {
            throw new IllegalArgumentException("Recipient account not found: " + toAccount);
        }

        BigDecimal newSenderBal = senderBal.subtract(amount);
        BigDecimal newReceiverBal = receiverBal.add(amount);

        // Update database balances
        databaseAccounts.put(fromAccount, newSenderBal);

        // Data consistency issue: Mutating external Redis cache inside an active database transaction.
        // If an exception occurs subsequently or the database transaction rolls back, Redis is not
        // rolled back, leaving phantom balances / corrupted dirty cache data in Redis.
        redisTemplate.opsForValue().set("wallet:" + fromAccount, new WalletBalance(fromAccount, newSenderBal));
        redisTemplate.opsForValue().set("wallet:" + toAccount, new WalletBalance(toAccount, newReceiverBal));

        // Simulated potential failure during secondary audit or database commit
        if (amount.compareTo(new BigDecimal("10000.00")) > 0) {
            throw new RuntimeException("Transaction flagged by fraud check! Rolling back DB transaction.");
        }

        databaseAccounts.put(toAccount, newReceiverBal);
    }

    // Concurrency issue: Directly updating cache instead of evicting creates race conditions
    // between concurrent writers where update order in DB differs from update order in Redis.
    public WalletBalance getBalance(String accountId) {
        WalletBalance cached = (WalletBalance) redisTemplate.opsForValue().get("wallet:" + accountId);
        if (cached != null) {
            return cached;
        }
        BigDecimal dbBal = databaseAccounts.get(accountId);
        if (dbBal == null) {
            return null;
        }
        WalletBalance balance = new WalletBalance(accountId, dbBal);
        redisTemplate.opsForValue().set("wallet:" + accountId, balance);
        return balance;
    }
}
```

## Issues identified

### 1. Dirty cache writes on database transaction rollback
- **Category:** Data consistency issue
- **Severity:** Critical
- **Explanation:** Mutating an external Redis cache inside a database transaction violates atomicity across heterogeneous stores. If any exception occurs after the Redis write (e.g. business validation, optimistic lock collision, network failure during DB commit), Spring rolls back the relational database changes. However, Redis operations cannot be rolled back by Spring's `PlatformTransactionManager`. The cache now permanently reflects uncommitted, phantom state.

### 2. Dual-write race conditions (update DB then update cache)
- **Category:** Concurrency issue
- **Severity:** High
- **Explanation:** When two threads $T_1$ and $T_2$ update the same record concurrently:
  1. $T_1$ writes DB balance $\$100$.
  2. $T_2$ writes DB balance $\$200$.
  3. Due to network latency or thread scheduling, $T_2$ writes Redis cache $\$200$.
  4. $T_1$ writes Redis cache $\$100$.
  Now DB holds $\$200$ but Redis permanently holds stale $\$100$. The standard solution is **Write to DB, then Evict Cache** (Cache-Aside with eviction).

### 3. Missing after-commit transaction synchronization
- **Category:** Transaction issue
- **Severity:** High
- **Explanation:** External cache invalidations must be registered via `TransactionSynchronization.afterCommit()` or `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` to guarantee eviction fires only after the database transaction has physically committed.

## Correct implementation

See `lab.cachingredis.dualwrite.SafeWalletService` under `src/main/java`.

## Trade-offs

- **Evict on After-Commit:** Guarantees no dirty cache writes on rollback, and resolves dual-write race conditions in majority of cases. Trade-off: The next request incurs a slight cache-miss penalty to reload state from the database.
- **Transactional Outbox for Cache Invalidation:** Publishing cache eviction events via transactional outbox or CDC (Debezium) guarantees at-least-once eviction even if the application crashes immediately after database commit.
