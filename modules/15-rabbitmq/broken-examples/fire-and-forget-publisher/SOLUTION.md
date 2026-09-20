# Solution — Fire-and-Forget Publishing

## Annotated code

```java
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

        // Reliability issue: Fire-and-forget publishing without publisher confirms or returns callback.
        // 1. If the exchange does not exist or the broker crashes, convertAndSend() returns void without error;
        //    the application assumes the audit log was delivered.
        // 2. If the routing key does not match any bound queues, the broker silently drops the message
        //    because mandatory flag and ReturnsCallback are not configured.
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, message);
    }
}
```

## Issue list

### Reliability issue: Fire-and-forget publishing risks undetected audit log loss

- **Location:** `AuditLogPublisher.java:27`
- **Description:** `rabbitTemplate.convertAndSend()` is called without configuring publisher confirms or return callbacks.
- **Impact:** In standard AMQP publishing, `convertAndSend` writes bytes to the TCP socket buffer and returns immediately. If the broker is unreachable, disk is full, or the target exchange does not exist, the publisher receives no error feedback. Furthermore, if the routing key does not match any queue bindings, RabbitMQ silently discards the message by default. In regulatory environments, lost security audit logs constitute severe compliance violations and data loss.
- **Remediation:** 
  1. Enable correlated publisher confirms (`spring.rabbitmq.publisher-confirm-type = correlated`) and supply a `CorrelationData` object with `convertAndSend()`. Await the confirm future or register a `ConfirmCallback` to verify broker disk persistence.
  2. Set `mandatory = true` and register a `ReturnsCallback` on `RabbitTemplate` so unroutable messages are returned to the application rather than silently dropped.
