package lab.kafka.broken.orderprocessing;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderPlacedEvent(
        String orderId,
        String customerId,
        BigDecimal amount,
        Instant occurredAt) {}
