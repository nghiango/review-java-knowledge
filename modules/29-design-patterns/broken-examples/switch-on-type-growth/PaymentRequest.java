package lab.designpatterns.broken.switchontype;

import java.math.BigDecimal;

public record PaymentRequest(
        String transactionId,
        String customerId,
        BigDecimal amount,
        PaymentType paymentType,
        String paymentDetails
) {}
