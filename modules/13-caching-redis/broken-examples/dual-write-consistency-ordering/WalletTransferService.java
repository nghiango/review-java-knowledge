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

        // Mutating Redis cache synchronously inside the open database transaction
        redisTemplate.opsForValue().set("wallet:" + fromAccount, new WalletBalance(fromAccount, newSenderBal));
        redisTemplate.opsForValue().set("wallet:" + toAccount, new WalletBalance(toAccount, newReceiverBal));

        // Simulated potential failure during secondary audit or database commit
        if (amount.compareTo(new BigDecimal("10000.00")) > 0) {
            throw new RuntimeException("Transaction flagged by fraud check! Rolling back DB transaction.");
        }

        databaseAccounts.put(toAccount, newReceiverBal);
    }

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
