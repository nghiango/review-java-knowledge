# Solution — Assuming Global Ordering Across Partitions

## Annotated code

### `UserEventProducer.java`

```java
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
        // Data consistency issue: Sending record without a message key (null key).
        // Kafka distributes unkeyed records across partitions using sticky batching or round-robin.
        // Events for the same user land on different partitions.
        kafkaTemplate.send(TOPIC, event);
    }

    public void publishUserUpdated(String userId, String details) {
        UserEvent event = new UserEvent(
                java.util.UUID.randomUUID().toString(),
                userId,
                "USER_UPDATED",
                details,
                java.time.Instant.now());
        // Data consistency issue: Without passing userId as partition key, this update will likely
        // go to a different partition than USER_CREATED or USER_DELETED.
        kafkaTemplate.send(TOPIC, event);
    }

    public void publishUserDeleted(String userId) {
        UserEvent event = new UserEvent(
                java.util.UUID.randomUUID().toString(),
                userId,
                "USER_DELETED",
                "{}",
                java.time.Instant.now());
        // Data consistency issue: Missing key breaks per-entity causal ordering.
        kafkaTemplate.send(TOPIC, event);
    }
}
```

### `UserActivityConsumer.java`

```java
package lab.kafka.broken.globalordering;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserActivityConsumer {

    private final UserStateRepository stateRepository;

    public UserActivityConsumer(UserStateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    // Concurrency issue: Listener runs with concurrency = 3 across multiple partitions.
    // Because producer did not key by userId, events for the same user are split across partitions.
    // Partition 1 may process USER_DELETED before Partition 0 processes USER_CREATED,
    // resurrecting deleted user records or throwing non-existent entity update errors.
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
```

## Issue list

### Data consistency issue: Missing partition key scatters user events across partitions

- **Location:** `UserEventProducer.java:26, 38, 50`
- **Description:** `kafkaTemplate.send(TOPIC, event)` is called without specifying a message key. The producer defaults to null-key partitioning.
- **Impact:** Kafka guarantees total ordering **only within a single partition**, never globally across multiple partitions of a topic. When `USER_CREATED`, `USER_UPDATED`, and `USER_DELETED` for user `U123` land in different partitions, consumers reading partitions in parallel will inevitably consume events out-of-order. A user might be marked deleted first, and subsequently recreated by a delayed `USER_CREATED` event, leaving zombie records in the database.
- **Remediation:** Pass `userId` as the partition key: `kafkaTemplate.send(TOPIC, userId, event)`. Kafka hashes the key using `murmur2`, guaranteeing that all events for that user are routed to the exact same partition in strict causal sequence.

### Concurrency issue: Multi-threaded consumer processes out-of-order unkeyed messages

- **Location:** `UserActivityConsumer.java:18-22`
- **Description:** The consumer spawns 3 concurrent listener threads for the multi-partition topic.
- **Impact:** Because the producer did not partition by user, parallel threads concurrently mutate the same user state without synchronization or ordering guarantees, causing race conditions and dirty overwrites.
- **Remediation:** Pair keyed partitioning on the producer with single-partition consumer processing or ensure all events for a given entity key map to a single consumer thread.
