package lab.kafka.idempotent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class SafeAccountCreditConsumerTest {

    @Mock private SafeAccountCreditConsumer.AccountBalanceRepository accountRepository;

    @Mock private DeduplicationStore deduplicationStore;

    @Mock private SafeAccountCreditConsumer.AuditLogService auditLogService;

    @Mock private Acknowledgment acknowledgment;

    @InjectMocks private SafeAccountCreditConsumer consumer;

    @Test
    @DisplayName("First execution of credit command updates balance and logs audit")
    void onCreditCommand_firstAttempt_updatesBalance() {
        CreditCommand command =
                new CreditCommand("tx-1", "acc-500", new BigDecimal("75.00"), Instant.now());

        when(deduplicationStore.tryMarkProcessed("tx-1")).thenReturn(true);

        boolean executed = consumer.onCreditCommand(command, acknowledgment);

        assertThat(executed).isTrue();
        verify(accountRepository).incrementBalance("acc-500", new BigDecimal("75.00"));
        verify(auditLogService).logCreditApplied("tx-1", "acc-500", new BigDecimal("75.00"));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName(
            "Duplicate redelivery of credit command is acknowledged and skips balance increment")
    void onCreditCommand_duplicateDelivery_skipsMutation() {
        CreditCommand command =
                new CreditCommand("tx-1", "acc-500", new BigDecimal("75.00"), Instant.now());

        when(deduplicationStore.tryMarkProcessed("tx-1")).thenReturn(false);

        boolean executed = consumer.onCreditCommand(command, acknowledgment);

        assertThat(executed).isFalse();
        verify(accountRepository, never()).incrementBalance(any(), any());
        verify(auditLogService, never()).logCreditApplied(any(), any(), any());
        verify(acknowledgment).acknowledge();
    }
}
