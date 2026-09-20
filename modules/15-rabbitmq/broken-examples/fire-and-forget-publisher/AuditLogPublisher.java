package lab.rabbitmq.broken.fireandforget;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditLogPublisher {

    public static final String EXCHANGE = "audit.events.exchange";
    public static final String ROUTING_KEY = "audit.security.auth";

    private final RabbitTemplate rabbitTemplate;

    public AuditLogPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishSecurityAudit(String eventId, String action, String principal, String details) {
        AuditMessage message = new AuditMessage(
                eventId,
                action,
                principal,
                details,
                java.time.Instant.now());

        // Fire-and-forget publishing without publisher confirms or returns callback
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, message);
    }
}
