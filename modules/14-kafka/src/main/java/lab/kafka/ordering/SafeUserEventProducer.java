package lab.kafka.ordering;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Production-grade producer guaranteeing causal event ordering per user.
 *
 * <p>Always supplies {@code userId} as the partition key to {@link KafkaTemplate#send(String,
 * Object, Object)}. Kafka's default partitioner hashes the key using Murmur2, ensuring all events
 * for the same user land in the identical partition in strict monotonic sequence.
 */
@Service
public class SafeUserEventProducer {

    public static final String TOPIC = "user-lifecycle-events";

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public SafeUserEventProducer(KafkaTemplate<String, UserEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<?> publishUserCreated(String userId, String details) {
        UserEvent event =
                new UserEvent(
                        UUID.randomUUID().toString(),
                        userId,
                        "USER_CREATED",
                        details,
                        Instant.now());
        // Keyed by userId: guarantees all events for this user route to the same partition
        return kafkaTemplate.send(TOPIC, userId, event);
    }

    public CompletableFuture<?> publishUserUpdated(String userId, String details) {
        UserEvent event =
                new UserEvent(
                        UUID.randomUUID().toString(),
                        userId,
                        "USER_UPDATED",
                        details,
                        Instant.now());
        // Keyed by userId
        return kafkaTemplate.send(TOPIC, userId, event);
    }

    public CompletableFuture<?> publishUserDeleted(String userId) {
        UserEvent event =
                new UserEvent(
                        UUID.randomUUID().toString(), userId, "USER_DELETED", "{}", Instant.now());
        // Keyed by userId
        return kafkaTemplate.send(TOPIC, userId, event);
    }
}
