package lab.rabbitmq.manualack;

import java.math.BigDecimal;

public record OrderNotification(
        String orderId, String customerId, BigDecimal totalAmount, String status) {}
