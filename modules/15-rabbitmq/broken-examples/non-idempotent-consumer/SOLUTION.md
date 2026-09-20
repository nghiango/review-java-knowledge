# Solution — Non-Idempotent Consumer Processing

## Annotated code

```java
package lab.rabbitmq.broken.nonidempotent;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class LoyaltyPointConsumer {

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyAuditService auditService;

    public LoyaltyPointConsumer(
            LoyaltyAccountRepository accountRepository,
            LoyaltyAuditService auditService) {
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }

    @RabbitListener(queues = "loyalty.points.award", ackMode = "MANUAL")
    public void onPointAward(
            PointAwardCommand command,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        // Data consistency issue: Non-idempotent consumer.
        // RabbitMQ guarantees at-least-once delivery. If the consumer crashes or network times out
        // before basicAck is registered by the broker, the message is redelivered with redelivered=true.
        // Directly invoking addPoints() without checking whether commandId was already processed
        // results in duplicate points being awarded to the user account.
        accountRepository.addPoints(command.userId(), command.points());
        auditService.recordPointsAwarded(command.commandId(), command.userId(), command.points());

        channel.basicAck(deliveryTag, false);
    }

    public interface LoyaltyAccountRepository {
        void addPoints(String userId, int points);
    }

    public interface LoyaltyAuditService {
        void recordPointsAwarded(String commandId, String userId, int points);
    }
}
```

## Issue list

### Data consistency issue: Missing deduplication causes duplicate state mutations on redelivery

- **Location:** `LoyaltyPointConsumer.java:27-29`
- **Description:** The listener processes incoming loyalty commands by mutating user account balances without verifying whether `command.commandId()` was previously processed.
- **Impact:** In RabbitMQ, if a consumer dies, encounters a network reset, or fails to ack within `consumer_timeout`, RabbitMQ closes the channel and requeues the unacknowledged message. The next consumer receives the message marked as redelivered. Executing `addPoints()` again doubles or triples the reward points given to the user, creating financial discrepancies and balance inflation.
- **Remediation:** Implement an idempotent consumer pattern. Verify `command.commandId()` against an atomic deduplication store (e.g. database table with unique constraint on `command_id` or Redis `SETNX`). If the ID was already recorded, acknowledge the message immediately and skip the point addition.
