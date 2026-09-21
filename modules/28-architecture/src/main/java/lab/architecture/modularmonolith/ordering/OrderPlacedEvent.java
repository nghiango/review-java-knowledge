package lab.architecture.modularmonolith.ordering;

import java.math.BigDecimal;

public record OrderPlacedEvent(String orderId, String customerId, BigDecimal amount) {}
