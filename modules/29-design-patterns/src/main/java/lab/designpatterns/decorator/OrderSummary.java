package lab.designpatterns.decorator;

import java.math.BigDecimal;
import java.util.Objects;

public record OrderSummary(
        String orderId, String customerId, BigDecimal totalAmount, boolean confidential) {
    public OrderSummary {
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(totalAmount);
    }
}
