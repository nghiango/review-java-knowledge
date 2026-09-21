package lab.architecture.modularmonolith.billing;

import java.math.BigDecimal;

public record OrderBilledEvent(String orderId, BigDecimal amount, boolean success) {}
