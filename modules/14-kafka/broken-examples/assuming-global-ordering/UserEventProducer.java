package lab.kafka.broken.globalordering;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserEventProducer {

    public static final String TOPIC = "user-lifecycle-events";

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public UserEventProducer(KafkaTemplate<String, UserEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserCreated(String userId, String details) {
        UserEvent event = new UserEvent(
                java.util.UUID.randomUUID().toString(),
                userId,
                "USER_CREATED",
                details,
                java.time.Instant.now());
        kafkaTemplate.send(TOPIC, event);
    }

    public void publishUserUpdated(String userId, String details) {
        UserEvent event = new UserEvent(
                java.util.UUID.randomUUID().toString(),
                userId,
                "USER_UPDATED",
                details,
                java.time.Instant.now());
        kafkaTemplate.send(TOPIC, event);
    }

    public void publishUserDeleted(String userId) {
        UserEvent event = new UserEvent(
                java.util.UUID.randomUUID().toString(),
                userId,
                "USER_DELETED",
                "{}",
                java.time.Instant.now());
        kafkaTemplate.send(TOPIC, event);
    }
}
