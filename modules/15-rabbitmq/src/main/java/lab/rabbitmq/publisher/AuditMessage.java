package lab.rabbitmq.publisher;

import java.time.Instant;

public record AuditMessage(
        String eventId, String action, String principal, String details, Instant occurredAt) {}
