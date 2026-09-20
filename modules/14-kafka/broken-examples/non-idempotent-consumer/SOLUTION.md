# Solution — Non-Idempotent Consumer Processing

## Annotated code

```java
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
        // Data consistency issue: Kafka guarantees at-least-once delivery by default.
        // If a consumer crashes before committing its offset, or if a rebalance occurs,
        // this method will receive the identical CreditCommand again.
        // Directly incrementing balance without checking whether transactionId was already processed
        // results in double-crediting funds to the customer account.
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
```

## Issue list

### Data consistency issue: Missing deduplication causes double-crediting on redelivery

- **Location:** `AccountCreditConsumer.java:23-25`
- **Description:** The listener processes incoming credit events by directly applying mutations without checking whether `command.transactionId()` has previously been executed.
- **Impact:** In distributed messaging, network timeouts, broker leader re-elections, and consumer group rebalances regularly cause duplicate message deliveries. A customer expecting a single \$1,000 credit will receive \$2,000 or \$3,000 if the consumer recovers from an offset commit timeout, leading to irreversible financial losses.
- **Remediation:** Implement an idempotent consumer pattern. Store processed `transactionId`s in a persistent deduplication store (e.g. unique constraint in a `processed_events` database table or atomic Redis key with TTL). Execute the balance update and event ID insertion atomically within the same database transaction. If the ID already exists, acknowledge the message and bypass execution.
