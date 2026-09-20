package lab.restapi.idempotency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID transactionId,
        String accountId,
        BigDecimal amount,
        String currency,
        String status,
        Instant processedAt) {}
