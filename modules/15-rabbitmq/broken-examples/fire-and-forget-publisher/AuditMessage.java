package lab.rabbitmq.broken.fireandforget;

import java.time.Instant;

public record AuditMessage(
        String eventId,
        String action,
        String principal,
        String details,
        Instant occurredAt) {}
