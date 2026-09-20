package lab.rabbitmq.idempotent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SafeLoyaltyPointConsumerTest {

    @Mock private SafeLoyaltyPointConsumer.LoyaltyAccountRepository accountRepository;

    @Mock private DeduplicationStore deduplicationStore;

    @Mock private SafeLoyaltyPointConsumer.LoyaltyAuditService auditService;

    @Mock private Channel channel;

    @InjectMocks private SafeLoyaltyPointConsumer consumer;

    @Test
    @DisplayName("First execution of point award updates balance and acknowledges")
    void onPointAward_firstExecution_addsPointsAndAcks() throws IOException {
        PointAwardCommand command = new PointAwardCommand("cmd-1", "user-100", 50, Instant.now());

        when(deduplicationStore.tryMarkProcessed("cmd-1")).thenReturn(true);

        boolean executed = consumer.onPointAward(command, channel, 25L);

        assertThat(executed).isTrue();
        verify(accountRepository).addPoints("user-100", 50);
        verify(auditService).recordPointsAwarded("cmd-1", "user-100", 50);
        verify(channel).basicAck(25L, false);
    }

    @Test
    @DisplayName("Duplicate redelivery acknowledges message and skips point addition")
    void onPointAward_duplicateRedelivery_skipsMutationAndAcks() throws IOException {
        PointAwardCommand command = new PointAwardCommand("cmd-1", "user-100", 50, Instant.now());

        when(deduplicationStore.tryMarkProcessed("cmd-1")).thenReturn(false);

        boolean executed = consumer.onPointAward(command, channel, 26L);

        assertThat(executed).isFalse();
        verify(accountRepository, never()).addPoints(any(), anyInt());
        verify(auditService, never()).recordPointsAwarded(any(), any(), anyInt());
        verify(channel).basicAck(26L, false);
    }
}
