package lab.distributeddata.questions;

import java.util.Map;

/**
 * Q26: How does Spring Modulith automate the Transactional Outbox pattern via EventPublicationRegistry?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q26SpringModulithOutboxEventPublicationExample {

    public static void main(String[] args) {
        // Spring Modulith provides built-in EventPublicationRegistry for transactional messaging:
        // 1. When an aggregate publishes an application event inside a @Transactional method:
        //    applicationEventPublisher.publishEvent(new OrderCompletedEvent(orderId));
        // 2. Spring Modulith intercepts the event and automatically serializes it to an internal
        //    event publication table (EVENT_PUBLICATION) within the SAME active database transaction.
        // 3. Downstream @ApplicationModuleListener methods or external message brokers (Kafka/RabbitMQ)
        //    process the event.
        // 4. Once the listener completes successfully, Spring Modulith marks the event publication as completed.
        // 5. Incompleted events upon crash are automatically re-dispatched upon application startup.

        Map<String, String> outboxFeatures =
                Map.of(
                        "Event Publication Registry", "Zero-boilerplate outbox table managed by Spring Data JDBC/JPA",
                        "Automatic Resubmission", "Recovers and republishes unacknowledged events after server restart");

        boolean automatesOutboxLifecycle =
                outboxFeatures.containsKey("Event Publication Registry"); // true
    }
}
