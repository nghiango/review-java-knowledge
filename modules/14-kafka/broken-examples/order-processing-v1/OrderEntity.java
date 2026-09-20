package lab.kafka.broken.orderprocessing;

import java.math.BigDecimal;

public record OrderEntity(
        String orderId,
        String customerId,
        BigDecimal totalAmount,
        String status) {}
