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
        // Read current balance without row lock or optimistic version check
        BigDecimal currentBalance =
                jdbcClient
                        .sql("SELECT balance FROM bank_accounts WHERE id = :id")
                        .param("id", accountId)
                        .query(BigDecimal.class)
                        .single();

        if (currentBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        // Calculate new balance in application memory
        BigDecimal newBalance = currentBalance.subtract(amount);

        // Write-back overwrites concurrent balance updates
        jdbcClient
                .sql("UPDATE bank_accounts SET balance = :newBalance WHERE id = :id")
                .param("newBalance", newBalance)
                .param("id", accountId)
                .update();
    }
}
