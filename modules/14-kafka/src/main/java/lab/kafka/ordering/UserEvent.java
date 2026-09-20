package lab.kafka.ordering;

import java.time.Instant;

public record UserEvent(
        String eventId, String userId, String eventType, String payload, Instant timestamp) {}
