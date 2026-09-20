package lab.rabbitmq.broken.nonidempotent;

import java.time.Instant;

public record PointAwardCommand(
        String commandId,
        String userId,
        int points,
        Instant timestamp) {}
