package lab.kafka.idempotent;

import java.math.BigDecimal;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Production-grade idempotent consumer protecting against duplicate message deliveries.
 *
 * <p>Checks the deduplication store before applying balance updates. Duplicate deliveries are
 * acknowledged immediately without executing balance mutations a second time.
 */
@Component
public class SafeAccountCreditConsumer {

    public static final String TOPIC = "account-credits";
    public static final String GROUP_ID = "credit-service-group";

    private final AccountBalanceRepository accountRepository;
    private final DeduplicationStore deduplicationStore;
    private final AuditLogService auditLogService;

    public SafeAccountCreditConsumer(
            AccountBalanceRepository accountRepository,
            DeduplicationStore deduplicationStore,
            AuditLogService auditLogService) {
        this.accountRepository = accountRepository;
        this.deduplicationStore = deduplicationStore;
        this.auditLogService = auditLogService;
    }

    @KafkaListener(topics = TOPIC, groupId = GROUP_ID)
    public boolean onCreditCommand(CreditCommand command, Acknowledgment ack) {
        // Atomic deduplication check
        boolean isFirstExecution = deduplicationStore.tryMarkProcessed(command.transactionId());
        if (!isFirstExecution) {
            // Duplicate detected: acknowledge offset and skip mutation
            if (ack != null) {
                ack.acknowledge();
            }
            return false;
        }

        accountRepository.incrementBalance(command.accountId(), command.amount());
        auditLogService.logCreditApplied(
                command.transactionId(), command.accountId(), command.amount());

        if (ack != null) {
            ack.acknowledge();
        }
        return true;
    }

    public interface AccountBalanceRepository {
        void incrementBalance(String accountId, BigDecimal amount);

        BigDecimal getBalance(String accountId);
    }

    public interface AuditLogService {
        void logCreditApplied(String transactionId, String accountId, BigDecimal amount);
    }
}
