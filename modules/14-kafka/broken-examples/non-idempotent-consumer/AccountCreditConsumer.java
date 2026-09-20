package lab.kafka.broken.nonidempotent;

import java.math.BigDecimal;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AccountCreditConsumer {

    private final AccountBalanceRepository accountRepository;
    private final AuditLogService auditLogService;

    public AccountCreditConsumer(
            AccountBalanceRepository accountRepository,
            AuditLogService auditLogService) {
        this.accountRepository = accountRepository;
        this.auditLogService = auditLogService;
    }

    @KafkaListener(topics = "account-credits", groupId = "credit-service-group")
    public void onCreditCommand(CreditCommand command) {
        accountRepository.incrementBalance(command.accountId(), command.amount());
        auditLogService.logCreditApplied(command.transactionId(), command.accountId(), command.amount());
    }

    public interface AccountBalanceRepository {
        void incrementBalance(String accountId, BigDecimal amount);
    }

    public interface AuditLogService {
        void logCreditApplied(String transactionId, String accountId, BigDecimal amount);
    }
}
