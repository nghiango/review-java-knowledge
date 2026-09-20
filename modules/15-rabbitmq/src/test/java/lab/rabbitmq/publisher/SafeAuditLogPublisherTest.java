package lab.rabbitmq.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class SafeAuditLogPublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;

    @Mock private SafeAuditLogPublisher.DeadLetterAuditService auditService;

    @InjectMocks private SafeAuditLogPublisher publisher;

    @Test
    @DisplayName("Returns true when broker acknowledges message with publisher confirm")
    void publishSecurityAudit_brokerAck_returnsTrue() {
        doAnswer(
                        invocation -> {
                            CorrelationData cd = invocation.getArgument(3);
                            cd.getFuture().complete(new CorrelationData.Confirm(true, null));
                            return null;
                        })
                .when(rabbitTemplate)
                .convertAndSend(
                        eq(SafeAuditLogPublisher.EXCHANGE),
                        eq(SafeAuditLogPublisher.ROUTING_KEY),
                        any(AuditMessage.class),
                        any(CorrelationData.class));

        boolean success =
                publisher.publishSecurityAudit(
                        "evt-1", "LOGIN_ATTEMPT", "admin", "Successful login", 2000L);

        assertThat(success).isTrue();
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("Records failure and returns false when broker nacks message")
    void publishSecurityAudit_brokerNack_returnsFalseAndRecordsAudit() {
        doAnswer(
                        invocation -> {
                            CorrelationData cd = invocation.getArgument(3);
                            cd.getFuture()
                                    .complete(new CorrelationData.Confirm(false, "Queue full"));
                            return null;
                        })
                .when(rabbitTemplate)
                .convertAndSend(
                        eq(SafeAuditLogPublisher.EXCHANGE),
                        eq(SafeAuditLogPublisher.ROUTING_KEY),
                        any(AuditMessage.class),
                        any(CorrelationData.class));

        boolean success =
                publisher.publishSecurityAudit(
                        "evt-2", "DATA_EXPORT", "user42", "Failed export", 2000L);

        assertThat(success).isFalse();
        verify(auditService).recordFailedPublish("evt-2", "Queue full");
    }
}
