package lab.kafka.ordering;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class SafeUserActivityConsumer {

    private final UserStateRepository stateRepository;

    public SafeUserActivityConsumer(UserStateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    @KafkaListener(
            topics = SafeUserEventProducer.TOPIC,
            groupId = "user-activity-consumer-group",
            concurrency = "3")
    public void onEvent(
            UserEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        // Because producer keyed by userId, all events for messageKey land on this specific
        // partition
        switch (event.eventType()) {
            case "USER_CREATED" -> stateRepository.createUser(event.userId(), event.payload());
            case "USER_UPDATED" -> stateRepository.updateUser(event.userId(), event.payload());
            case "USER_DELETED" -> stateRepository.deleteUser(event.userId());
            default ->
                    throw new IllegalArgumentException("Unknown event type: " + event.eventType());
        }
    }

    public interface UserStateRepository {
        void createUser(String userId, String payload);

        void updateUser(String userId, String payload);

        void deleteUser(String userId);

        String getUserState(String userId);
    }
}
