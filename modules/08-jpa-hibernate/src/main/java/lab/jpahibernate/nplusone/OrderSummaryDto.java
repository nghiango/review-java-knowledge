package lab.jpahibernate.nplusone;

import java.math.BigDecimal;

public record OrderSummaryDto(
        Long orderId, String customerName, int itemCount, BigDecimal totalAmount) {}
