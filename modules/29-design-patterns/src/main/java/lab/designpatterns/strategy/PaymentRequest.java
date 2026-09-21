package lab.designpatterns.strategy;

import java.math.BigDecimal;
import java.util.Objects;

public record PaymentRequest(
        String transactionId,
        String customerId,
        BigDecimal amount,
        PaymentType paymentType,
        String paymentDetails) {
    public PaymentRequest {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(paymentType, "paymentType must not be null");
    }
}
