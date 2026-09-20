package lab.rabbitmq.publisher;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Production-grade RabbitMQ publisher with publisher confirms and return callbacks.
 *
 * <p>Uses {@link CorrelationData} to verify that RabbitMQ has safely written the message to
 * disk/replicated to Quorum queues, and validates routing to eliminate silent message loss.
 */
@Service
public class SafeAuditLogPublisher {

    public static final String EXCHANGE = "audit.events.exchange";
    public static final String ROUTING_KEY = "audit.security.auth";

    private final RabbitTemplate rabbitTemplate;
    private final DeadLetterAuditService auditService;

    public SafeAuditLogPublisher(
            RabbitTemplate rabbitTemplate, DeadLetterAuditService auditService) {
        this.rabbitTemplate = rabbitTemplate;
        this.auditService = auditService;
    }

    public boolean publishSecurityAudit(
            String eventId, String action, String principal, String details, long timeoutMillis) {
        AuditMessage message = new AuditMessage(eventId, action, principal, details, Instant.now());
        CorrelationData correlationData = new CorrelationData(eventId);

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, message, correlationData);

        try {
            CompletableFuture<CorrelationData.Confirm> future = correlationData.getFuture();
            CorrelationData.Confirm confirm = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            if (confirm != null && confirm.isAck()) {
                return true;
            } else {
                String reason = (confirm != null) ? confirm.getReason() : "No confirm received";
                auditService.recordFailedPublish(eventId, reason);
                return false;
            }
        } catch (Exception ex) {
            auditService.recordFailedPublish(eventId, ex.getMessage());
            return false;
        }
    }

    public interface DeadLetterAuditService {
        void recordFailedPublish(String eventId, String reason);
    }
}
