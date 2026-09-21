package lab.architecture.bff;

import java.math.BigDecimal;
import java.util.Objects;
import lab.architecture.bff.dto.MobileOrderSummaryDto;
import lab.architecture.modularmonolith.shipping.ShippingService;
import org.springframework.stereotype.Component;

/**
 * Backend for Frontend (BFF) specialized for Mobile iOS / Android clients.
 *
 * <p>Responsibilities: - Aggregates data across backend services into a single HTTP round-trip. -
 * Shapes and trims data to reduce cellular bandwidth and mobile memory usage. - Does NOT contain
 * core business invariants or duplicate domain logic.
 */
@Component
public class MobileOrderBffController {

    private final ShippingService shippingService;

    public MobileOrderBffController(ShippingService shippingService) {
        this.shippingService =
                Objects.requireNonNull(shippingService, "ShippingService must not be null");
    }

    public MobileOrderSummaryDto getOrderForMobile(
            String orderId, BigDecimal amount, int itemCount) {
        String shipmentStatus = shippingService.getShipmentStatus(orderId);
        String estimatedDelivery =
                "PREPARING_DISPATCH".equals(shipmentStatus) ? "In 2 days" : "Pending";

        // Returns compact, mobile-optimized presentation payload
        return new MobileOrderSummaryDto(
                orderId, shipmentStatus, amount, itemCount, estimatedDelivery);
    }
}
