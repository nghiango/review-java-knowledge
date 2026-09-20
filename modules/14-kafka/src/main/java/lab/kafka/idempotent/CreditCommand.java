package lab.kafka.idempotent;

import java.math.BigDecimal;
import java.time.Instant;

public record CreditCommand(
        String transactionId, String accountId, BigDecimal amount, Instant timestamp) {}
