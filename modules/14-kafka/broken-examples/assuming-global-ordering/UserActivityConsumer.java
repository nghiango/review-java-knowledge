package lab.kafka.broken.globalordering;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserActivityConsumer {

    private final UserStateRepository stateRepository;

    public UserActivityConsumer(UserStateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    @KafkaListener(
            topics = UserEventProducer.TOPIC,
            groupId = "user-activity-consumer-group",
            concurrency = "3")
    public void onEvent(UserEvent event) {
        switch (event.eventType()) {
            case "USER_CREATED" -> stateRepository.createUser(event.userId(), event.payload());
            case "USER_UPDATED" -> stateRepository.updateUser(event.userId(), event.payload());
            case "USER_DELETED" -> stateRepository.deleteUser(event.userId());
            default -> throw new IllegalArgumentException("Unknown event type: " + event.eventType());
        }
    }

    public interface UserStateRepository {
        void createUser(String userId, String payload);

        void updateUser(String userId, String payload);

        void deleteUser(String userId);
    }
}
