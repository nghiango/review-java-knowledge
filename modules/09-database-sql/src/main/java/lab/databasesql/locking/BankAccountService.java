package lab.databasesql.locking;

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

    public record AccountRecord(Long id, BigDecimal balance, long version) {}

    /**
     * Fix 1: Atomic database-level update prevents Lost Updates without application-level locking.
     */
    @Transactional
    public void withdrawAtomic(Long accountId, BigDecimal amount) {
        int rowsUpdated =
                jdbcClient
                        .sql(
                                "UPDATE bank_accounts "
                                        + "SET balance = balance - :amount "
                                        + "WHERE id = :id AND balance >= :amount")
                        .param("amount", amount)
                        .param("id", accountId)
                        .update();

        if (rowsUpdated == 0) {
            throw new IllegalStateException(
                    "Insufficient funds or account not found for id: " + accountId);
        }
    }

    /**
     * Fix 2: Multi-resource transfer with deterministic lock ordering to prevent Coffman deadlocks.
     */
    @Transactional
    public void transferWithPessimisticLocking(
            Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Canonical lock ordering: always lock the smaller ID first
        Long firstId = Math.min(fromAccountId, toAccountId);
        Long secondId = Math.max(fromAccountId, toAccountId);

        jdbcClient
                .sql("SELECT id, balance FROM bank_accounts WHERE id = :id FOR UPDATE")
                .param("id", firstId)
                .query(AccountRecord.class)
                .single();

        jdbcClient
                .sql("SELECT id, balance FROM bank_accounts WHERE id = :id FOR UPDATE")
                .param("id", secondId)
                .query(AccountRecord.class)
                .single();

        // Perform balance updates safely
        withdrawAtomic(fromAccountId, amount);

        jdbcClient
                .sql("UPDATE bank_accounts SET balance = balance + :amount WHERE id = :id")
                .param("amount", amount)
                .param("id", toAccountId)
                .update();
    }
}
