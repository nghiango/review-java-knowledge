package lab.distributeddata.outbox;

import java.time.Instant;

public record OutboxEvent(
        String id,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        String status,
        Instant createdAt) {}
