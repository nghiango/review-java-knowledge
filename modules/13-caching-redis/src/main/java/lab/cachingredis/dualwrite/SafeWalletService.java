package lab.cachingredis.dualwrite;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class SafeWalletService {

    private final Map<String, BigDecimal> databaseAccounts = new ConcurrentHashMap<>();
    private final Map<String, WalletBalance> redisCache = new ConcurrentHashMap<>();

    public SafeWalletService() {
        databaseAccounts.put("acc-101", new BigDecimal("1000.00"));
        databaseAccounts.put("acc-102", new BigDecimal("500.00"));
        redisCache.put("wallet:acc-101", new WalletBalance("acc-101", new BigDecimal("1000.00")));
        redisCache.put("wallet:acc-102", new WalletBalance("acc-102", new BigDecimal("500.00")));
    }

    // Cache-Aside with Post-Commit Eviction:
    // 1. Mutate relational database inside transaction.
    // 2. Do NOT write to cache directly (prevents concurrent write race conditions).
    // 3. Register afterCommit synchronization to evict cache entries ONLY IF transaction physically
    // commits.
    // 4. If transaction rolls back, afterCommit never executes, leaving cache clean of phantom
    // uncommitted data.
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

        // Simulated potential failure or fraud rejection triggering rollback
        if (amount.compareTo(new BigDecimal("10000.00")) > 0) {
            throw new IllegalStateException("Fraud detected! Transaction aborted.");
        }

        databaseAccounts.put(toAccount, newReceiverBal);

        // Evict cache only after database transaction commit
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            redisCache.remove("wallet:" + fromAccount);
                            redisCache.remove("wallet:" + toAccount);
                        }
                    });
        } else {
            redisCache.remove("wallet:" + fromAccount);
            redisCache.remove("wallet:" + toAccount);
        }
    }

    public WalletBalance getBalance(String accountId) {
        WalletBalance cached = redisCache.get("wallet:" + accountId);
        if (cached != null) {
            return cached;
        }

        BigDecimal dbBal = databaseAccounts.get(accountId);
        if (dbBal == null) {
            return null;
        }

        WalletBalance fresh = new WalletBalance(accountId, dbBal);
        redisCache.put("wallet:" + accountId, fresh);
        return fresh;
    }

    public Map<String, WalletBalance> getCacheState() {
        return redisCache;
    }

    public Map<String, BigDecimal> getDatabaseState() {
        return databaseAccounts;
    }
}
