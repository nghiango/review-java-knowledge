package lab.kafka.broken.ackbeforeprocessing;

import java.math.BigDecimal;

public record PaymentNotification(String paymentId, String orderId, BigDecimal amount, String status) {}
