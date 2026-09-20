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
