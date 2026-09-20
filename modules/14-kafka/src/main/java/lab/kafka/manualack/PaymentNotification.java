package lab.kafka.manualack;

import java.math.BigDecimal;

public record PaymentNotification(
        String paymentId, String orderId, BigDecimal amount, String status) {}
