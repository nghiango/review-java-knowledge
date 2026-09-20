package lab.rabbitmq.idempotent;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Production-grade idempotent consumer for RabbitMQ.
 *
 * <p>Validates {@code commandId} against an atomic deduplication store. Redelivered duplicate
 * messages are acknowledged immediately and ignored.
 */
@Component
public class SafeLoyaltyPointConsumer {

    public static final String QUEUE = "loyalty.points.award";

    private final LoyaltyAccountRepository accountRepository;
    private final DeduplicationStore deduplicationStore;
    private final LoyaltyAuditService auditService;

    public SafeLoyaltyPointConsumer(
            LoyaltyAccountRepository accountRepository,
            DeduplicationStore deduplicationStore,
            LoyaltyAuditService auditService) {
        this.accountRepository = accountRepository;
        this.deduplicationStore = deduplicationStore;
        this.auditService = auditService;
    }

    @RabbitListener(queues = QUEUE, ackMode = "MANUAL")
    public boolean onPointAward(
            PointAwardCommand command,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {
        // Atomic idempotency test-and-set
        if (!deduplicationStore.tryMarkProcessed(command.commandId())) {
            // Duplicate message detected: acknowledge and discard
            channel.basicAck(deliveryTag, false);
            return false;
        }

        accountRepository.addPoints(command.userId(), command.points());
        auditService.recordPointsAwarded(command.commandId(), command.userId(), command.points());

        channel.basicAck(deliveryTag, false);
        return true;
    }

    public interface LoyaltyAccountRepository {
        void addPoints(String userId, int points);

        int getPoints(String userId);
    }

    public interface LoyaltyAuditService {
        void recordPointsAwarded(String commandId, String userId, int points);
    }
}
