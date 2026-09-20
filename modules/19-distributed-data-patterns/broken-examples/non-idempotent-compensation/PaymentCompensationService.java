package lab.distributeddata.broken.compensation;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCompensationService {

    private final AccountRepository accountRepository;
    private final PaymentAuditRepository auditRepository;

    public PaymentCompensationService(AccountRepository accountRepository, PaymentAuditRepository auditRepository) {
        this.accountRepository = accountRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public void compensateFailedOrder(String orderId, String accountId, BigDecimal amountToRefund) {
        Account account = accountRepository.findById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }

        account.setBalance(account.getBalance().add(amountToRefund));
        accountRepository.save(account);

        auditRepository.recordRefund(orderId, accountId, amountToRefund);
    }

    public static class Account {
        private final String accountId;
        private BigDecimal balance;

        public Account(String accountId, BigDecimal balance) {
            this.accountId = accountId;
            this.balance = balance;
        }

        public String getAccountId() { return accountId; }
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
    }

    public interface AccountRepository {
        Account findById(String accountId);
        void save(Account account);
    }

    public interface PaymentAuditRepository {
        void recordRefund(String orderId, String accountId, BigDecimal amount);
    }
}
