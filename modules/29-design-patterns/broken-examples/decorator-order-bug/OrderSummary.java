package lab.designpatterns.broken.decoratororder;

import java.math.BigDecimal;

public record OrderSummary(String orderId, String customerId, BigDecimal totalAmount, boolean confidential) {}
