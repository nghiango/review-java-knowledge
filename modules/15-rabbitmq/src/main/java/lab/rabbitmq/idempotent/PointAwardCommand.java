package lab.rabbitmq.idempotent;

import java.time.Instant;

public record PointAwardCommand(String commandId, String userId, int points, Instant timestamp) {}
