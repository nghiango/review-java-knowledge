package lab.architecture.bff;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import lab.architecture.bff.dto.WebOrderDetailDto;
import lab.architecture.bff.dto.WebOrderDetailDto.WebOrderItemDto;
import lab.architecture.modularmonolith.shipping.ShippingService;
import org.springframework.stereotype.Component;

/**
 * Backend for Frontend (BFF) specialized for Desktop Web clients.
 *
 * <p>Responsibilities: - Provides comprehensive, richly structured responses for high-bandwidth
 * browser clients. - Formats currencies, tax summaries, tracking information, and full itemized
 * lists. - Preserves presentation separation without leaking domain entity models directly to web
 * clients.
 */
@Component
public class WebOrderBffController {

    private final ShippingService shippingService;

    public WebOrderBffController(ShippingService shippingService) {
        this.shippingService =
                Objects.requireNonNull(shippingService, "ShippingService must not be null");
    }

    public WebOrderDetailDto getOrderForWeb(
            String orderId,
            String customerId,
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal shippingCost,
            List<WebOrderItemDto> items) {
        String shipmentStatus = shippingService.getShipmentStatus(orderId);
        BigDecimal grandTotal = subtotal.add(tax).add(shippingCost);

        return new WebOrderDetailDto(
                orderId,
                customerId,
                shipmentStatus,
                subtotal,
                tax,
                shippingCost,
                grandTotal,
                "USD",
                items,
                "TRK-" + orderId,
                "FedEx Ground",
                "Delivering Friday by 7 PM");
    }
}
