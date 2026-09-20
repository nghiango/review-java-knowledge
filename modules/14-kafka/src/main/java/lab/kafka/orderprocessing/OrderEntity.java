package lab.kafka.orderprocessing;

import java.math.BigDecimal;

public record OrderEntity(
        String orderId, String customerId, BigDecimal totalAmount, String status) {}
