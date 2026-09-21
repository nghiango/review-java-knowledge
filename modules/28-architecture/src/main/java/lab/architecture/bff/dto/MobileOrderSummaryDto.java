package lab.architecture.bff.dto;

import java.math.BigDecimal;

/**
 * Lightweight, bandwidth-optimized DTO designed specifically for mobile clients. Strips
 * non-essential catalog descriptions, detailed tax breakdowns, and internal IDs to minimize
 * cellular payload size and mobile battery consumption.
 */
public record MobileOrderSummaryDto(
        String orderId,
        String status,
        BigDecimal totalAmount,
        int totalItems,
        String estimatedDeliveryDate) {}
