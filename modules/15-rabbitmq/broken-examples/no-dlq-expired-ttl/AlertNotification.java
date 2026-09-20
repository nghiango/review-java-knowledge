package lab.rabbitmq.broken.nodlq;

import java.time.Instant;

public record AlertNotification(
        String alertId,
        String severity,
        String message,
        Instant timestamp) {}
