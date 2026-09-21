package lab.architecture.bff.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rich desktop web presentation DTO. Includes complete item breakdown, unit prices, tax
 * calculations, shipping tracking numbers, and historical state timestamps.
 */
public record WebOrderDetailDto(
        String orderId,
        String customerId,
        String status,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal shippingCost,
        BigDecimal grandTotal,
        String currency,
        List<WebOrderItemDto> items,
        String trackingNumber,
        String carrier,
        String estimatedDeliveryDate) {
    public record WebOrderItemDto(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {}
}
